package rush_bot;

import battlecode.common.*;

import static rush_bot.Actions.*;
import static rush_bot.Communication.*;
import static rush_bot.Debug.*;
import static rush_bot.Globals.*;
import static rush_bot.Map.*;
import static rush_bot.Nav.*;
import static rush_bot.Utils.*;
import static rush_bot.Wall.*;
import static rush_bot.Zones.*;

public class BotLandscaperRush extends BotLandscaper {

    public static RobotInfo netGunInfo = null;

    public static void loop() throws GameActionException {
        while (true) {
            try {
                Globals.update();

                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Globals.endTurn();
        }
    }

    public static void turn() throws GameActionException {
        log("RUSH ROLE");
        // check if netgun is protected
        // finds netgun
        if (netGunInfo == null) {
            for (RobotInfo ri: visibleAllies) {
                if (ri.type == RobotType.NET_GUN && maxXYDistance(enemyHQLoc, ri.location) <= 2) {
                    netGunInfo = ri;
                    break;
                }
            }
        }

        // checks if netgun is protected by at least 4 ally landscapers
        if (netGunInfo != null) {
            if (here.isAdjacentTo(netGunInfo.location)) {
                doProtectRole();
                return;
            } else {
                log("netgun:");
                int[] counts = countAdjacentLandscapers(netGunInfo.location);
                int openCount = counts[0];
                int enemyLandscaperCount = counts[1];
                int allyLandscaperCount = counts[2];
                if (openCount == 0) {
                    doRushRole();
                    return;
                }
                if (enemyLandscaperCount > openCount + allyLandscaperCount) {
                    doRushRole();
                    return;
                }
                if (enemyRush || allyLandscaperCount >= enemyLandscaperCount + openCount) {
                    doRushRole();
                    return;
                }

                doProtectRole();
                return;
            }
        }

        doRushRole();
        return;
    }

    private static void doProtectRole() throws GameActionException {
        log("PROTECT");
        // checks if netgun is protected by at least 4 ally landscapers
        if (here.isAdjacentTo(netGunInfo.location)) {
            if (rc.getDirtCarrying() == 0) {
                rushDig();
                return;
            }
            // save myself from flooding
            if(rc.senseElevation(here) - waterLevel < 3) {
                Actions.doDepositDirt(Direction.CENTER);
                return;
            }
            // protect netgun if already adjacent to it
            if (netGunInfo.dirtCarrying > 0) {
                if (rc.getDirtCarrying() < myType.dirtLimit) {
                    Actions.doDigDirt(here.directionTo(netGunInfo.location));
                    return;
                } else {
                    protectDeposit();
                    return;
                }
            }

            // try to kill adjacent enemy buildings
            RobotInfo bestEnemy = null;
            int bestScore = N_INF;
            for (RobotInfo ri: adjacentEnemies) {
                if (ri.type.isBuilding()) {
                    int score = 0;
                    int[] counts = countAdjacentLandscapers(ri.location);
                    int openCount = counts[0];
                    int enemyLandscaperCount = counts[1];
                    int allyLandscaperCount = counts[2];
                    if (ri.type == RobotType.HQ) {
                        if (allyLandscaperCount > enemyLandscaperCount + openCount) {
                            score = P_INF;
                        } else if (allyLandscaperCount > enemyLandscaperCount) {
                            score = 30000 + allyLandscaperCount - enemyLandscaperCount;
                        } else {
                            score = 100 + allyLandscaperCount - enemyLandscaperCount;
                        }
                    } else {
                        if (allyLandscaperCount > enemyLandscaperCount + openCount) {
                            score = P_INF / 2;
                        } else if (allyLandscaperCount > enemyLandscaperCount) {
                            score = 2000 + allyLandscaperCount - enemyLandscaperCount;
                        } else {
                            score = allyLandscaperCount - enemyLandscaperCount;
                        }
                    }
                    if (score > bestScore) {
                        bestScore = score;
                        bestEnemy = ri;
                    }
                }
            }
            if (bestEnemy != null) {
                Actions.doDepositDirt(here.directionTo(bestEnemy.location));
                return;
            }

            // when the netgun has no dirt on it, try to maintain 2/3 dirt capacity
            if (rc.getDirtCarrying() < myType.dirtLimit * 2 / 3) {
                rushDig();
                return;
            } else {
                protectDeposit();
                return;
            }
        } else {
            // STATE == not adj to netgun
            for (Direction dir: directions) {
                MapLocation loc = here.add(dir);
                if (!rc.onTheMap(loc)) {
                    continue;
                }
                if (loc.isAdjacentTo(netGunInfo.location)) {
                    if (isLocDryFlatEmpty(loc)) {
                        log("Force move to netgun");
                        Actions.doMove(dir);
                        return;
                    }
                }
            }
            log("Bug nav to netgun");
            moveLog(netGunInfo.location);
            return;
        }

    }

    public static int[] countAdjacentLandscapers (MapLocation targetLoc) throws GameActionException {
        int openCount = 0;
        int enemyLandscaperCount = 0;
        int allyLandscaperCount = 0;
        for (Direction dir: directions) {
            MapLocation loc = targetLoc.add(dir);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            if (!rc.canSenseLocation(loc)) {
                openCount++;
                continue;
            }
            if (isLocEmpty(loc)) {
                openCount++;
                continue;
            } else {
                RobotInfo ri = rc.senseRobotAtLocation(loc);
                if (ri.type == RobotType.LANDSCAPER) {
                    if (ri.team == us) {
                        allyLandscaperCount++;
                    } else {
                        enemyLandscaperCount++;
                    }
                }
                continue;
            }
        }
        log(targetLoc + ": o e a " + openCount + " " + enemyLandscaperCount + " " + allyLandscaperCount);
        return new int[] {openCount, enemyLandscaperCount, allyLandscaperCount};
    }

    private static void protectDeposit() throws GameActionException {
        Direction bestDir = null;
        int bestScore = N_INF;
        for (Direction dir: allDirections) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            int score = 0;
            if (isLocEmpty(loc)) {
                // prioritize empty tiles farther from enemy HQ
                score = loc.distanceSquaredTo(enemyHQLoc);
            } else {
                RobotInfo ri = rc.senseRobotAtLocation(loc);
                if (ri.team == us) {
                    if (ri.type.isBuilding()) {
                        score = N_INF;
                    } else {
                        // prioritize allies closer to enemy HQ
                        score = -loc.distanceSquaredTo(enemyHQLoc);
                    }
                } else {
                    if (ri.type.isBuilding()) {
                        score = P_INF;
                    } else {
                        score = P_INF / 2;
                    }
                }
            }
            if (score > bestScore) {
                bestDir = dir;
                bestScore = score;
            }
        }
        Actions.doDepositDirt(bestDir);
        return;
    }

    private static void doRushRole() throws GameActionException {
        log("RUSH");
        // if so, then rush try to kill enemy HQ
        if (here.isAdjacentTo(enemyHQLoc)) {
            if (rc.getDirtCarrying() > 0) {
                Actions.doDepositDirt(here.directionTo(enemyHQLoc));
                return;
            } else {
                rushDig();
                return;
            }
        } else {
            boolean tryRush = true;
            log("enemy hq:");
            int[] counts = countAdjacentLandscapers(enemyHQLoc);
            int openCount = counts[0];
            int enemyLandscaperCount = counts[1];
            int allyLandscaperCount = counts[2];
            if (openCount == 0) {
                tryRush = false;
            }
            if (enemyLandscaperCount >= openCount + allyLandscaperCount) {
                // at least half are protected by enemy landscapers
                tryRush = false;
            }
            if (tryRush) {
                for (Direction dir: directions) {
                    MapLocation loc = rc.adjacentLocation(dir);
                    if (!rc.onTheMap(loc)) {
                        continue;
                    }
                    if (loc.isAdjacentTo(enemyHQLoc)) {
                        if (isLocDryFlatEmpty(loc)) {
                            Actions.doMove(dir);
                            return;
                        }
                    }
                }
                moveLog(enemyHQLoc);
                return;
            } else {
                log("HARASS");
                // STATE = not trying to rush, instead harass
                // attack enemy buildings
                int minDist = P_INF;
                MapLocation minEnemy = null;
                for (RobotInfo ri: visibleEnemies) {
                    if (ri.type.isBuilding() && ri.type != RobotType.HQ) {
                        int dist = here.distanceSquaredTo(ri.location);
                        if (dist < minDist) {
                            minDist = dist;
                            minEnemy = ri.location;
                        }
                    }
                }

                if (minEnemy != null) {
                    if (here.isAdjacentTo(minEnemy)) {
                        if (rc.getDirtCarrying() > 0) {
                            protectDeposit();
                            return;
                        } else {
                            rushDig();
                            return;
                        }
                    }
                    moveLog(minEnemy);
                    return;
                } else {
                    moveLog(enemyHQLoc);
                    return;
                }
            }
        }
    }

    private static void rushDig () throws GameActionException {
        Direction bestDir = null;
        int bestScore = N_INF;
        for (Direction dir: allDirections) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            int score = 0;
            if (isLocEmpty(loc)) {
                // prioritize empty tiles farther from enemy HQ
                score = loc.distanceSquaredTo(enemyHQLoc);
            } else {
                RobotInfo ri = rc.senseRobotAtLocation(loc);
                if (ri.team == us) {
                    if (ri.type.isBuilding()) {
                        if (ri.dirtCarrying > 0) {
                            score = P_INF;
                        } else {
                            continue;
                        }
                    } else {
                        // prioritize allies closer to enemy HQ
                        score = -loc.distanceSquaredTo(enemyHQLoc);
                    }
                } else {
                    if (ri.type.isBuilding()) {
                        continue;
                    } else {
                        score = P_INF / 2;
                    }
                }
            }
            if (score > bestScore) {
                bestDir = dir;
                bestScore = score;
            }
        }
        Actions.doDigDirt(bestDir);
        return;
    }
}
