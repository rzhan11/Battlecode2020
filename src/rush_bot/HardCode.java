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

public class HardCode {

	/*
	Assigns an array to 'senseDirections' that contains all of the senseable dx/dy/mag positions for a given RobotType
	Sorted by distanceSquared
	*/
	public static void getSenseDirections(RobotType rt) {
		switch (rt) {
			case DELIVERY_DRONE: // sensorRadiusSquared = 24
			case DESIGN_SCHOOL: // sensorRadiusSquared = 24
			case FULFILLMENT_CENTER: // sensorRadiusSquared = 24
			case LANDSCAPER: // sensorRadiusSquared = 24
			case NET_GUN: // sensorRadiusSquared = 24
			case REFINERY: // sensorRadiusSquared = 24
			case VAPORATOR: // sensorRadiusSquared = 24
				senseDirections = new int[][] {{0,0,0},{-1,0,1},{0,-1,1},{0,1,1},{1,0,1},{-1,-1,2},{-1,1,2},{1,-1,2},{1,1,2},{-2,0,4},{0,-2,4},{0,2,4},{2,0,4},{-2,-1,5},{-2,1,5},{-1,-2,5},{-1,2,5},{1,-2,5},{1,2,5},{2,-1,5},{2,1,5},{-2,-2,8},{-2,2,8},{2,-2,8},{2,2,8},{-3,0,9},{0,-3,9},{0,3,9},{3,0,9},{-3,-1,10},{-3,1,10},{-1,-3,10},{-1,3,10},{1,-3,10},{1,3,10},{3,-1,10},{3,1,10},{-3,-2,13},{-3,2,13},{-2,-3,13},{-2,3,13},{2,-3,13},{2,3,13},{3,-2,13},{3,2,13},{-4,0,16},{0,-4,16},{0,4,16},{4,0,16},{-4,-1,17},{-4,1,17},{-1,-4,17},{-1,4,17},{1,-4,17},{1,4,17},{4,-1,17},{4,1,17},{-3,-3,18},{-3,3,18},{3,-3,18},{3,3,18},{-4,-2,20},{-4,2,20},{-2,-4,20},{-2,4,20},{2,-4,20},{2,4,20},{4,-2,20},{4,2,20}};
				break;
			case HQ: // sensorRadiusSquared = 48
				senseDirections = new int[][] {{0,0,0},{-1,0,1},{0,-1,1},{0,1,1},{1,0,1},{-1,-1,2},{-1,1,2},{1,-1,2},{1,1,2},{-2,0,4},{0,-2,4},{0,2,4},{2,0,4},{-2,-1,5},{-2,1,5},{-1,-2,5},{-1,2,5},{1,-2,5},{1,2,5},{2,-1,5},{2,1,5},{-2,-2,8},{-2,2,8},{2,-2,8},{2,2,8},{-3,0,9},{0,-3,9},{0,3,9},{3,0,9},{-3,-1,10},{-3,1,10},{-1,-3,10},{-1,3,10},{1,-3,10},{1,3,10},{3,-1,10},{3,1,10},{-3,-2,13},{-3,2,13},{-2,-3,13},{-2,3,13},{2,-3,13},{2,3,13},{3,-2,13},{3,2,13},{-4,0,16},{0,-4,16},{0,4,16},{4,0,16},{-4,-1,17},{-4,1,17},{-1,-4,17},{-1,4,17},{1,-4,17},{1,4,17},{4,-1,17},{4,1,17},{-3,-3,18},{-3,3,18},{3,-3,18},{3,3,18},{-4,-2,20},{-4,2,20},{-2,-4,20},{-2,4,20},{2,-4,20},{2,4,20},{4,-2,20},{4,2,20},{-5,0,25},{-4,-3,25},{-4,3,25},{-3,-4,25},{-3,4,25},{0,-5,25},{0,5,25},{3,-4,25},{3,4,25},{4,-3,25},{4,3,25},{5,0,25},{-5,-1,26},{-5,1,26},{-1,-5,26},{-1,5,26},{1,-5,26},{1,5,26},{5,-1,26},{5,1,26},{-5,-2,29},{-5,2,29},{-2,-5,29},{-2,5,29},{2,-5,29},{2,5,29},{5,-2,29},{5,2,29},{-4,-4,32},{-4,4,32},{4,-4,32},{4,4,32},{-5,-3,34},{-5,3,34},{-3,-5,34},{-3,5,34},{3,-5,34},{3,5,34},{5,-3,34},{5,3,34},{-6,0,36},{0,-6,36},{0,6,36},{6,0,36},{-6,-1,37},{-6,1,37},{-1,-6,37},{-1,6,37},{1,-6,37},{1,6,37},{6,-1,37},{6,1,37},{-6,-2,40},{-6,2,40},{-2,-6,40},{-2,6,40},{2,-6,40},{2,6,40},{6,-2,40},{6,2,40},{-5,-4,41},{-5,4,41},{-4,-5,41},{-4,5,41},{4,-5,41},{4,5,41},{5,-4,41},{5,4,41},{-6,-3,45},{-6,3,45},{-3,-6,45},{-3,6,45},{3,-6,45},{3,6,45},{6,-3,45},{6,3,45}};
				break;
			case MINER: // sensorRadiusSquared = 35
				senseDirections = new int[][] {{0,0,0},{-1,0,1},{0,-1,1},{0,1,1},{1,0,1},{-1,-1,2},{-1,1,2},{1,-1,2},{1,1,2},{-2,0,4},{0,-2,4},{0,2,4},{2,0,4},{-2,-1,5},{-2,1,5},{-1,-2,5},{-1,2,5},{1,-2,5},{1,2,5},{2,-1,5},{2,1,5},{-2,-2,8},{-2,2,8},{2,-2,8},{2,2,8},{-3,0,9},{0,-3,9},{0,3,9},{3,0,9},{-3,-1,10},{-3,1,10},{-1,-3,10},{-1,3,10},{1,-3,10},{1,3,10},{3,-1,10},{3,1,10},{-3,-2,13},{-3,2,13},{-2,-3,13},{-2,3,13},{2,-3,13},{2,3,13},{3,-2,13},{3,2,13},{-4,0,16},{0,-4,16},{0,4,16},{4,0,16},{-4,-1,17},{-4,1,17},{-1,-4,17},{-1,4,17},{1,-4,17},{1,4,17},{4,-1,17},{4,1,17},{-3,-3,18},{-3,3,18},{3,-3,18},{3,3,18},{-4,-2,20},{-4,2,20},{-2,-4,20},{-2,4,20},{2,-4,20},{2,4,20},{4,-2,20},{4,2,20},{-5,0,25},{-4,-3,25},{-4,3,25},{-3,-4,25},{-3,4,25},{0,-5,25},{0,5,25},{3,-4,25},{3,4,25},{4,-3,25},{4,3,25},{5,0,25},{-5,-1,26},{-5,1,26},{-1,-5,26},{-1,5,26},{1,-5,26},{1,5,26},{5,-1,26},{5,1,26},{-5,-2,29},{-5,2,29},{-2,-5,29},{-2,5,29},{2,-5,29},{2,5,29},{5,-2,29},{5,2,29},{-4,-4,32},{-4,4,32},{4,-4,32},{4,4,32},{-5,-3,34},{-5,3,34},{-3,-5,34},{-3,5,34},{3,-5,34},{3,5,34},{5,-3,34},{5,3,34}};
				break;
			default:
				logi("ERROR: Sanity check failed - cannot find hardcoded senseDirections for RobotType " + rt);
				break;
		}
	}

	/*
	Returns an array that contains all of dx/dy/mag positions that a Net Gun/HQ can shoot
	Sorted by distanceSquared
	Current shoot radius = 15
	*/
	public static int[][] getShootDirections () {
		return new int[][] {{0,0,0},{-1,0,1},{0,-1,1},{0,1,1},{1,0,1},{-1,-1,2},{-1,1,2},{1,-1,2},{1,1,2},{-2,0,4},{0,-2,4},{0,2,4},{2,0,4},{-2,-1,5},{-2,1,5},{-1,-2,5},{-1,2,5},{1,-2,5},{1,2,5},{2,-1,5},{2,1,5},{-2,-2,8},{-2,2,8},{2,-2,8},{2,2,8},{-3,0,9},{0,-3,9},{0,3,9},{3,0,9},{-3,-1,10},{-3,1,10},{-1,-3,10},{-1,3,10},{1,-3,10},{1,3,10},{3,-1,10},{3,1,10},{-3,-2,13},{-3,2,13},{-2,-3,13},{-2,3,13},{2,-3,13},{2,3,13},{3,-2,13},{3,2,13}};
	}

	/*
	For a given elevation, returns the round number in which it will be flooded
	 */
	public static int getRoundFlooded(int elevation) {
		if (elevation <= 0) {
			return 0;
		}
		if (elevation >= 5000) {
			return P_INF;
		}
		switch(elevation){
			case 0:return 1;case 1:return 256;case 2:return 464;case 3:return 677;case 4:return 931;case 5:return 1210;case 6:return 1413;case 7:return 1546;case 8:return 1640;case 9:return 1713;case 10:return 1771;case 11:return 1819;case 12:return 1861;case 13:return 1897;case 14:return 1929;case 15:return 1957;case 16:return 1983;case 17:return 2007;case 18:return 2028;case 19:return 2048;case 20:return 2067;case 21:return 2084;case 22:return 2100;case 23:return 2115;case 24:return 2129;case 25:return 2143;case 26:return 2155;case 27:return 2168;case 28:return 2179;case 29:return 2190;case 30:return 2201;case 31:return 2211;case 32:return 2220;case 33:return 2230;case 34:return 2239;case 35:return 2247;case 36:return 2256;case 37:return 2264;case 38:return 2271;case 39:return 2279;case 40:return 2286;case 41:return 2293;case 42:return 2300;case 43:return 2307;case 44:return 2313;case 45:return 2319;case 46:return 2325;case 47:return 2331;case 48:return 2337;case 49:return 2343;case 50:return 2348;case 51:return 2353;case 52:return 2359;case 53:return 2364;case 54:return 2369;case 55:return 2374;case 56:return 2379;case 57:return 2383;case 58:return 2388;case 59:return 2392;case 60:return 2397;case 61:return 2401;case 62:return 2405;case 63:return 2409;case 64:return 2413;case 65:return 2417;case 66:return 2421;case 67:return 2425;case 68:return 2429;case 69:return 2433;case 70:return 2436;case 71:return 2440;case 72:return 2443;case 73:return 2447;case 74:return 2450;case 75:return 2454;case 76:return 2457;case 77:return 2460;case 78:return 2464;case 79:return 2467;case 80:return 2470;case 81:return 2473;case 82:return 2476;case 83:return 2479;case 84:return 2482;case 85:return 2485;case 86:return 2488;case 87:return 2491;case 88:return 2493;case 89:return 2496;case 90:return 2499;case 91:return 2502;case 92:return 2504;case 93:return 2507;case 94:return 2509;case 95:return 2512;case 96:return 2514;case 97:return 2517;case 98:return 2519;case 99:return 2522;case 100:return 2524;case 101:return 2527;case 102:return 2529;case 103:return 2531;case 104:return 2534;case 105:return 2536;case 106:return 2538;case 107:return 2540;case 108:return 2543;case 109:return 2545;case 110:return 2547;case 111:return 2549;case 112:return 2551;case 113:return 2553;case 114:return 2555;case 115:return 2557;case 116:return 2559;case 117:return 2561;case 118:return 2563;case 119:return 2565;case 120:return 2567;case 121:return 2569;case 122:return 2571;case 123:return 2573;case 124:return 2575;case 125:return 2577;case 126:return 2579;case 127:return 2581;case 128:return 2582;case 129:return 2584;case 130:return 2586;case 131:return 2588;case 132:return 2590;case 133:return 2591;case 134:return 2593;case 135:return 2595;case 136:return 2596;case 137:return 2598;case 138:return 2600;case 139:return 2601;case 140:return 2603;case 141:return 2605;case 142:return 2606;case 143:return 2608;case 144:return 2610;case 145:return 2611;case 146:return 2613;case 147:return 2614;case 148:return 2616;case 149:return 2617;case 150:return 2619;case 151:return 2620;case 152:return 2622;case 153:return 2623;case 154:return 2625;case 155:return 2626;case 156:return 2628;case 157:return 2629;case 158:return 2631;case 159:return 2632;case 160:return 2633;case 161:return 2635;case 162:return 2636;case 163:return 2638;case 164:return 2639;case 165:return 2640;case 166:return 2642;case 167:return 2643;case 168:return 2644;case 169:return 2646;case 170:return 2647;case 171:return 2648;case 172:return 2650;case 173:return 2651;case 174:return 2652;case 175:return 2654;case 176:return 2655;case 177:return 2656;case 178:return 2657;case 179:return 2659;case 180:return 2660;case 181:return 2661;case 182:return 2662;case 183:return 2664;case 184:return 2665;case 185:return 2666;case 186:return 2667;case 187:return 2668;case 188:return 2670;case 189:return 2671;case 190:return 2672;case 191:return 2673;case 192:return 2674;case 193:return 2675;case 194:return 2677;case 195:return 2678;case 196:return 2679;case 197:return 2680;case 198:return 2681;case 199:return 2682;case 200:return 2683;case 201:return 2684;case 202:return 2685;case 203:return 2687;case 204:return 2688;case 205:return 2689;case 206:return 2690;case 207:return 2691;case 208:return 2692;case 209:return 2693;case 210:return 2694;case 211:return 2695;case 212:return 2696;case 213:return 2697;case 214:return 2698;case 215:return 2699;case 216:return 2700;case 217:return 2701;case 218:return 2702;case 219:return 2703;case 220:return 2704;case 221:return 2705;case 222:return 2706;case 223:return 2707;case 224:return 2708;case 225:return 2709;case 226:return 2710;case 227:return 2711;case 228:return 2712;case 229:return 2713;case 230:return 2714;case 231:return 2715;case 232:return 2716;case 233:return 2717;case 234:return 2718;case 235:return 2719;case 236:return 2719;case 237:return 2720;case 238:return 2721;case 239:return 2722;case 240:return 2723;case 241:return 2724;case 242:return 2725;case 243:return 2726;case 244:return 2727;case 245:return 2728;case 246:return 2728;case 247:return 2729;case 248:return 2730;case 249:return 2731;case 250:return 2732;case 251:return 2733;case 252:return 2734;case 253:return 2735;case 254:return 2735;case 255:return 2736;case 256:return 2737;case 257:return 2738;case 258:return 2739;case 259:return 2740;case 260:return 2740;case 261:return 2741;case 262:return 2742;case 263:return 2743;case 264:return 2744;case 265:return 2744;case 266:return 2745;case 267:return 2746;case 268:return 2747;case 269:return 2748;case 270:return 2749;case 271:return 2749;case 272:return 2750;case 273:return 2751;case 274:return 2752;case 275:return 2752;case 276:return 2753;case 277:return 2754;case 278:return 2755;case 279:return 2756;case 280:return 2756;case 281:return 2757;case 282:return 2758;case 283:return 2759;case 284:return 2759;case 285:return 2760;case 286:return 2761;case 287:return 2762;case 288:return 2762;case 289:return 2763;case 290:return 2764;case 291:return 2765;case 292:return 2765;case 293:return 2766;case 294:return 2767;case 295:return 2767;case 296:return 2768;case 297:return 2769;case 298:return 2770;case 299:return 2770;case 300:return 2771;case 301:return 2772;case 302:return 2772;case 303:return 2773;case 304:return 2774;case 305:return 2775;case 306:return 2775;case 307:return 2776;case 308:return 2777;case 309:return 2777;case 310:return 2778;case 311:return 2779;case 312:return 2779;case 313:return 2780;case 314:return 2781;case 315:return 2781;case 316:return 2782;case 317:return 2783;case 318:return 2783;case 319:return 2784;case 320:return 2785;case 321:return 2785;case 322:return 2786;case 323:return 2787;case 324:return 2787;case 325:return 2788;case 326:return 2789;case 327:return 2789;case 328:return 2790;case 329:return 2791;case 330:return 2791;case 331:return 2792;case 332:return 2793;case 333:return 2793;case 334:return 2794;case 335:return 2794;case 336:return 2795;case 337:return 2796;case 338:return 2796;case 339:return 2797;case 340:return 2798;case 341:return 2798;case 342:return 2799;case 343:return 2799;case 344:return 2800;case 345:return 2801;case 346:return 2801;case 347:return 2802;case 348:return 2803;case 349:return 2803;case 350:return 2804;case 351:return 2804;case 352:return 2805;case 353:return 2806;case 354:return 2806;case 355:return 2807;case 356:return 2807;case 357:return 2808;case 358:return 2808;case 359:return 2809;case 360:return 2810;case 361:return 2810;case 362:return 2811;case 363:return 2811;case 364:return 2812;case 365:return 2813;case 366:return 2813;case 367:return 2814;case 368:return 2814;case 369:return 2815;case 370:return 2815;case 371:return 2816;case 372:return 2817;case 373:return 2817;case 374:return 2818;case 375:return 2818;case 376:return 2819;case 377:return 2819;case 378:return 2820;case 379:return 2820;case 380:return 2821;case 381:return 2822;case 382:return 2822;case 383:return 2823;case 384:return 2823;case 385:return 2824;case 386:return 2824;case 387:return 2825;case 388:return 2825;case 389:return 2826;case 390:return 2826;case 391:return 2827;case 392:return 2828;case 393:return 2828;case 394:return 2829;case 395:return 2829;case 396:return 2830;case 397:return 2830;case 398:return 2831;case 399:return 2831;case 400:return 2832;case 401:return 2832;case 402:return 2833;case 403:return 2833;case 404:return 2834;case 405:return 2834;case 406:return 2835;case 407:return 2835;case 408:return 2836;case 409:return 2836;case 410:return 2837;case 411:return 2837;case 412:return 2838;case 413:return 2838;case 414:return 2839;case 415:return 2839;case 416:return 2840;case 417:return 2840;case 418:return 2841;case 419:return 2841;case 420:return 2842;case 421:return 2842;case 422:return 2843;case 423:return 2843;case 424:return 2844;case 425:return 2844;case 426:return 2845;case 427:return 2845;case 428:return 2846;case 429:return 2846;case 430:return 2847;case 431:return 2847;case 432:return 2848;case 433:return 2848;case 434:return 2849;case 435:return 2849;case 436:return 2850;case 437:return 2850;case 438:return 2851;case 439:return 2851;case 440:return 2852;case 441:return 2852;case 442:return 2853;case 443:return 2853;case 444:return 2853;case 445:return 2854;case 446:return 2854;case 447:return 2855;case 448:return 2855;case 449:return 2856;case 450:return 2856;case 451:return 2857;case 452:return 2857;case 453:return 2858;case 454:return 2858;case 455:return 2859;case 456:return 2859;case 457:return 2859;case 458:return 2860;case 459:return 2860;case 460:return 2861;case 461:return 2861;case 462:return 2862;case 463:return 2862;case 464:return 2863;case 465:return 2863;case 466:return 2864;case 467:return 2864;case 468:return 2864;case 469:return 2865;case 470:return 2865;case 471:return 2866;case 472:return 2866;case 473:return 2867;case 474:return 2867;case 475:return 2867;case 476:return 2868;case 477:return 2868;case 478:return 2869;case 479:return 2869;case 480:return 2870;case 481:return 2870;case 482:return 2871;case 483:return 2871;case 484:return 2871;case 485:return 2872;case 486:return 2872;case 487:return 2873;case 488:return 2873;case 489:return 2873;case 490:return 2874;case 491:return 2874;case 492:return 2875;case 493:return 2875;case 494:return 2876;case 495:return 2876;case 496:return 2876;case 497:return 2877;case 498:return 2877;case 499:return 2878;case 500:return 2878;case 501:return 2879;case 502:return 2879;case 503:return 2879;case 504:return 2880;case 505:return 2880;case 506:return 2881;case 507:return 2881;case 508:return 2881;case 509:return 2882;case 510:return 2882;case 511:return 2883;case 512:return 2883;case 513:return 2883;case 514:return 2884;case 515:return 2884;case 516:return 2885;case 517:return 2885;case 518:return 2885;case 519:return 2886;case 520:return 2886;case 521:return 2887;case 522:return 2887;case 523:return 2887;case 524:return 2888;case 525:return 2888;case 526:return 2889;case 527:return 2889;case 528:return 2889;case 529:return 2890;case 530:return 2890;case 531:return 2890;case 532:return 2891;case 533:return 2891;case 534:return 2892;case 535:return 2892;case 536:return 2892;case 537:return 2893;case 538:return 2893;case 539:return 2894;case 540:return 2894;case 541:return 2894;case 542:return 2895;case 543:return 2895;case 544:return 2895;case 545:return 2896;case 546:return 2896;case 547:return 2897;case 548:return 2897;case 549:return 2897;case 550:return 2898;case 551:return 2898;case 552:return 2898;case 553:return 2899;case 554:return 2899;case 555:return 2900;case 556:return 2900;case 557:return 2900;case 558:return 2901;case 559:return 2901;case 560:return 2901;case 561:return 2902;case 562:return 2902;case 563:return 2903;case 564:return 2903;case 565:return 2903;case 566:return 2904;case 567:return 2904;case 568:return 2904;case 569:return 2905;case 570:return 2905;case 571:return 2905;case 572:return 2906;case 573:return 2906;case 574:return 2907;case 575:return 2907;case 576:return 2907;case 577:return 2908;case 578:return 2908;case 579:return 2908;case 580:return 2909;case 581:return 2909;case 582:return 2909;case 583:return 2910;case 584:return 2910;case 585:return 2910;case 586:return 2911;case 587:return 2911;case 588:return 2911;case 589:return 2912;case 590:return 2912;case 591:return 2912;case 592:return 2913;case 593:return 2913;case 594:return 2914;case 595:return 2914;case 596:return 2914;case 597:return 2915;case 598:return 2915;case 599:return 2915;case 600:return 2916;case 601:return 2916;case 602:return 2916;case 603:return 2917;case 604:return 2917;case 605:return 2917;case 606:return 2918;case 607:return 2918;case 608:return 2918;case 609:return 2919;case 610:return 2919;case 611:return 2919;case 612:return 2920;case 613:return 2920;case 614:return 2920;case 615:return 2921;case 616:return 2921;case 617:return 2921;case 618:return 2922;case 619:return 2922;case 620:return 2922;case 621:return 2923;case 622:return 2923;case 623:return 2923;case 624:return 2924;case 625:return 2924;case 626:return 2924;case 627:return 2925;case 628:return 2925;case 629:return 2925;case 630:return 2926;case 631:return 2926;case 632:return 2926;case 633:return 2927;case 634:return 2927;case 635:return 2927;case 636:return 2928;case 637:return 2928;case 638:return 2928;case 639:return 2928;case 640:return 2929;case 641:return 2929;case 642:return 2929;case 643:return 2930;case 644:return 2930;case 645:return 2930;case 646:return 2931;case 647:return 2931;case 648:return 2931;case 649:return 2932;case 650:return 2932;case 651:return 2932;case 652:return 2933;case 653:return 2933;case 654:return 2933;case 655:return 2934;case 656:return 2934;case 657:return 2934;case 658:return 2934;case 659:return 2935;case 660:return 2935;case 661:return 2935;case 662:return 2936;case 663:return 2936;case 664:return 2936;case 665:return 2937;case 666:return 2937;case 667:return 2937;case 668:return 2938;case 669:return 2938;case 670:return 2938;case 671:return 2938;case 672:return 2939;case 673:return 2939;case 674:return 2939;case 675:return 2940;case 676:return 2940;case 677:return 2940;case 678:return 2941;case 679:return 2941;case 680:return 2941;case 681:return 2941;case 682:return 2942;case 683:return 2942;case 684:return 2942;case 685:return 2943;case 686:return 2943;case 687:return 2943;case 688:return 2944;case 689:return 2944;case 690:return 2944;case 691:return 2944;case 692:return 2945;case 693:return 2945;case 694:return 2945;case 695:return 2946;case 696:return 2946;case 697:return 2946;case 698:return 2946;case 699:return 2947;case 700:return 2947;case 701:return 2947;case 702:return 2948;case 703:return 2948;case 704:return 2948;case 705:return 2949;case 706:return 2949;case 707:return 2949;case 708:return 2949;case 709:return 2950;case 710:return 2950;case 711:return 2950;case 712:return 2951;case 713:return 2951;case 714:return 2951;case 715:return 2951;case 716:return 2952;case 717:return 2952;case 718:return 2952;case 719:return 2953;case 720:return 2953;case 721:return 2953;case 722:return 2953;case 723:return 2954;case 724:return 2954;case 725:return 2954;case 726:return 2955;case 727:return 2955;case 728:return 2955;case 729:return 2955;case 730:return 2956;case 731:return 2956;case 732:return 2956;case 733:return 2956;case 734:return 2957;case 735:return 2957;case 736:return 2957;case 737:return 2958;case 738:return 2958;case 739:return 2958;case 740:return 2958;case 741:return 2959;case 742:return 2959;case 743:return 2959;case 744:return 2959;case 745:return 2960;case 746:return 2960;case 747:return 2960;case 748:return 2961;case 749:return 2961;case 750:return 2961;case 751:return 2961;case 752:return 2962;case 753:return 2962;case 754:return 2962;case 755:return 2962;case 756:return 2963;case 757:return 2963;case 758:return 2963;case 759:return 2964;case 760:return 2964;case 761:return 2964;case 762:return 2964;case 763:return 2965;case 764:return 2965;case 765:return 2965;case 766:return 2965;case 767:return 2966;case 768:return 2966;case 769:return 2966;case 770:return 2966;case 771:return 2967;case 772:return 2967;case 773:return 2967;case 774:return 2968;case 775:return 2968;case 776:return 2968;case 777:return 2968;case 778:return 2969;case 779:return 2969;case 780:return 2969;case 781:return 2969;case 782:return 2970;case 783:return 2970;case 784:return 2970;case 785:return 2970;case 786:return 2971;case 787:return 2971;case 788:return 2971;case 789:return 2971;case 790:return 2972;case 791:return 2972;case 792:return 2972;case 793:return 2972;case 794:return 2973;case 795:return 2973;case 796:return 2973;case 797:return 2973;case 798:return 2974;case 799:return 2974;case 800:return 2974;case 801:return 2974;case 802:return 2975;case 803:return 2975;case 804:return 2975;case 805:return 2975;case 806:return 2976;case 807:return 2976;case 808:return 2976;case 809:return 2976;case 810:return 2977;case 811:return 2977;case 812:return 2977;case 813:return 2977;case 814:return 2978;case 815:return 2978;case 816:return 2978;case 817:return 2978;case 818:return 2979;case 819:return 2979;case 820:return 2979;case 821:return 2979;case 822:return 2980;case 823:return 2980;case 824:return 2980;case 825:return 2980;case 826:return 2981;case 827:return 2981;case 828:return 2981;case 829:return 2981;case 830:return 2982;case 831:return 2982;case 832:return 2982;case 833:return 2982;case 834:return 2983;case 835:return 2983;case 836:return 2983;case 837:return 2983;case 838:return 2984;case 839:return 2984;case 840:return 2984;case 841:return 2984;case 842:return 2985;case 843:return 2985;case 844:return 2985;case 845:return 2985;case 846:return 2986;case 847:return 2986;case 848:return 2986;case 849:return 2986;case 850:return 2987;case 851:return 2987;case 852:return 2987;case 853:return 2987;case 854:return 2987;case 855:return 2988;case 856:return 2988;case 857:return 2988;case 858:return 2988;case 859:return 2989;case 860:return 2989;case 861:return 2989;case 862:return 2989;case 863:return 2990;case 864:return 2990;case 865:return 2990;case 866:return 2990;case 867:return 2991;case 868:return 2991;case 869:return 2991;case 870:return 2991;case 871:return 2991;case 872:return 2992;case 873:return 2992;case 874:return 2992;case 875:return 2992;case 876:return 2993;case 877:return 2993;case 878:return 2993;case 879:return 2993;case 880:return 2994;case 881:return 2994;case 882:return 2994;case 883:return 2994;case 884:return 2994;case 885:return 2995;case 886:return 2995;case 887:return 2995;case 888:return 2995;case 889:return 2996;case 890:return 2996;case 891:return 2996;case 892:return 2996;case 893:return 2997;case 894:return 2997;case 895:return 2997;case 896:return 2997;case 897:return 2997;case 898:return 2998;case 899:return 2998;case 900:return 2998;case 901:return 2998;case 902:return 2999;case 903:return 2999;case 904:return 2999;case 905:return 2999;case 906:return 2999;case 907:return 3000;case 908:return 3000;case 909:return 3000;case 910:return 3000;case 911:return 3001;case 912:return 3001;case 913:return 3001;case 914:return 3001;case 915:return 3001;case 916:return 3002;case 917:return 3002;case 918:return 3002;case 919:return 3002;case 920:return 3003;case 921:return 3003;case 922:return 3003;case 923:return 3003;case 924:return 3003;case 925:return 3004;case 926:return 3004;case 927:return 3004;case 928:return 3004;case 929:return 3005;case 930:return 3005;case 931:return 3005;case 932:return 3005;case 933:return 3005;case 934:return 3006;case 935:return 3006;case 936:return 3006;case 937:return 3006;case 938:return 3006;case 939:return 3007;case 940:return 3007;case 941:return 3007;case 942:return 3007;case 943:return 3008;case 944:return 3008;case 945:return 3008;case 946:return 3008;case 947:return 3008;case 948:return 3009;case 949:return 3009;case 950:return 3009;case 951:return 3009;case 952:return 3009;case 953:return 3010;case 954:return 3010;case 955:return 3010;case 956:return 3010;case 957:return 3011;case 958:return 3011;case 959:return 3011;case 960:return 3011;case 961:return 3011;case 962:return 3012;case 963:return 3012;case 964:return 3012;case 965:return 3012;case 966:return 3012;case 967:return 3013;case 968:return 3013;case 969:return 3013;case 970:return 3013;case 971:return 3013;case 972:return 3014;case 973:return 3014;case 974:return 3014;case 975:return 3014;case 976:return 3014;case 977:return 3015;case 978:return 3015;case 979:return 3015;case 980:return 3015;case 981:return 3016;case 982:return 3016;case 983:return 3016;case 984:return 3016;case 985:return 3016;case 986:return 3017;case 987:return 3017;case 988:return 3017;case 989:return 3017;case 990:return 3017;case 991:return 3018;case 992:return 3018;case 993:return 3018;case 994:return 3018;case 995:return 3018;case 996:return 3019;case 997:return 3019;case 998:return 3019;case 999:return 3019;}
		return 0;
	}
}
