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
        if (maxXYDistance(here, enemyHQLoc) > 1) {
            int openCount = 0;
            int enemyLandscaperCount = 0;
            int allyLandscaperCount = 0;
            for (Direction dir: directions) {
                MapLocation loc = enemyHQLoc.add(dir);
                if (!rc.onTheMap(loc)) {
                    continue;
                }
                if (!rc.canSenseLocation(loc)) {
                    openCount++;
                    continue;
                }
                if (isLocDryEmpty(loc)) {
                    openCount++;
                }
                RobotInfo ri = rc.senseRobotAtLocation(loc);
                if (ri != null && ri.type == RobotType.LANDSCAPER) {
                    if (ri.team == us) {
                        allyLandscaperCount++;
                    } else {
                        enemyLandscaperCount++;
                    }
                }
            }
            log("o e a " + openCount + " " + enemyLandscaperCount + " " + allyLandscaperCount);
            if (openCount == 0) {
                doProtectRole();
                return;
            }
            if (enemyLandscaperCount >= openCount + allyLandscaperCount) {
                // at least half are protected by enemy landscapers
                doProtectRole();
                return;
            }
            // if not being rushed, only require a simple majority
            // if being rushed, try to fill in all empty spots around enemy HQ
            if (!enemyRush) {
                if (allyLandscaperCount > enemyLandscaperCount + openCount) {
                    // at least half are protected by enemy landscapers
                    doProtectRole();
                    return;
                }
            }
        }
        doRushRole();
    }

    public static boolean netGunProtected = false;

    private static void doProtectRole() throws GameActionException {
        log("PROTECT");
        // finds netgun
        RobotInfo netGunInfo = null;
        for (RobotInfo ri: visibleAllies) {
            if (ri.type == RobotType.NET_GUN && maxXYDistance(enemyHQLoc, ri.location) <= 2) {
                netGunInfo = ri;
                break;
            }
        }
        // checks if netgun is protected by at least 4 ally landscapers
        if (netGunInfo != null) {
            // protect netgun if already adjacent to it
            if (here.isAdjacentTo(netGunInfo.location)) {
                if (netGunInfo.dirtCarrying > 0) {
                    if (rc.getDirtCarrying() < myType.dirtLimit) {
                        rushDig();
                        return;
                    } else {
                        protectDeposit();
                        return;
                    }
                }
                // try to kill adjacent enemy buildings
                for (RobotInfo ri: adjacentEnemies) {
                    if (ri.type.isBuilding()) {
                        if (rc.getDirtCarrying() > 0) {
                            protectDeposit();
                            return;
                        } else {
                            rushDig();
                            return;
                        }
                    }
                }
                // when the netgun has no dirt on it, try to maintain 2/3 dirt capacity
                if (rc.getDirtCarrying() < myType.dirtLimit * 2 / 3) {
                    rushDig();
                    return;
                } else {
                    protectDeposit();
                    return;
                }
            }

            // STATE == not adj to netgun
            int allyLandscaperCount = 0;
            for (Direction dir: directions) {
                MapLocation loc = netGunInfo.location.add(dir);
                if (!rc.onTheMap(loc)) {
                    continue;
                }
                if (rc.canSenseLocation(loc) && isLocAllyLandscaper(loc)) {
                    allyLandscaperCount++;
                    break;
                }
            }
            if (allyLandscaperCount < 4) {
                // STATE = netgun is not protected by more allies
                moveLog(netGunInfo.location);
                return;
            }
        }
        // STATE = netgun is protected
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
            moveLog(minEnemy);
            return;
        } else {
            moveLog(enemyHQLoc);
            return;
        }
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
        if (here.isAdjacentTo(enemyHQLoc)) {
            if (rc.getDirtCarrying() > 0) {
                Actions.doDepositDirt(here.directionTo(enemyHQLoc));
                return;
            } else {
                rushDig();
                return;
            }
        } else {
            moveLog(enemyHQLoc);
            return;
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
