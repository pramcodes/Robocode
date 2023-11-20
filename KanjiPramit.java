package MyBots;
import robocode.*;
import java.awt.*;
import robocode.util.*;
import java.awt.geom.*;

/**
 * based on SuperSpinBotby CrazyBassoonist which was also based on the robot Spinbot by Mathew Nelson and maintained by Flemming N. Larsen
 * Main Stratergies: Wall smoothing movement, Circular targeting , bullet dodging by random movement**/

public class KanjiPramit extends AdvancedRobot {
	//gun variables
	static double enemyVelocities[][]=new double[400][4];
	static int currentEnemyVelocity;
	static int aimingEnemyVelocity;
	double velocityToAimAt;
	boolean fired;
	double oldTime;
	int count;
	int averageCount;
	
	//movement variables
	static double turn=2;
	int turnDir=1;
	int moveDir=1;
	double oldEnemyHeading;
	double oldEnergy=100;

	public void run(){
		// Set colors
		setBodyColor(Color.black);
		setGunColor(Color.red);
		setRadarColor(Color.black);
		setScanColor(Color.red);

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		do{
			turnRadarRightRadians(Double.POSITIVE_INFINITY);
		}while(true);
	}
	public void onScannedRobot(ScannedRobotEvent e){
		double absBearing=e.getBearingRadians()+getHeadingRadians();
		Graphics2D g=getGraphics();

		// ram into enemy when its energy has ran out(disabled) for ram bonus points
		if (e.getEnergy() < 1){
			if (e.getBearing() >= 0) {
				moveDir = 1;
			} else {
				moveDir = -1;
			}
			setMaxTurnRate(8);
			setMaxVelocity(8);
			turnRight(e.getBearing());
			setAhead(e.getDistance() + 5);
			setBack(40);
			scan();
			
		}
		else{
			//increase our turn speed amount each tick,to a maximum of 8 and a minimum of 4
			turn+=0.2*Math.random();
			if(turn>8){
				turn=2;
            }
			
			//when the enemy fires, we try to dodge by randomly changing turn direction 
			if(oldEnergy-e.getEnergy() <= 3 && oldEnergy-e.getEnergy() >= 0.1){
				if(Math.random()>.5){
					turnDir*=-1;
				}
				if(Math.random()>.8){
					moveDir*=-1;
				}
			}
			
			//we set our maximum speed to go down as our turn rate goes up so that when we turn slowly, we speed up and vice versa;
			setMaxTurnRate(turn);
			setMaxVelocity(12-turn);
            
			//wall smoothing movement from robowiki 
			double goalDirection = absBearing-Math.PI/2*moveDir; // this is the absolute heading I want to move in to go clockwise or counterclockwise
			Rectangle2D fieldRect = new Rectangle2D.Double(18, 18, getBattleFieldWidth()-36, getBattleFieldHeight()-36);
			while (!fieldRect.contains(getX()+Math.sin(goalDirection)*120, getY()+ Math.cos(goalDirection)*120))
			{
				goalDirection += moveDir*.1;	//turn a little toward enemy and try again
			}
			double turn = robocode.util.Utils.normalRelativeAngle(goalDirection-getHeadingRadians());
			if (Math.abs(turn) > Math.PI/2){
				turn = robocode.util.Utils.normalRelativeAngle(turn + Math.PI);
				setBack(100);
			}else
				setAhead(100);

			setTurnRightRadians(turn);

			oldEnergy=e.getEnergy();
			
			//target 
			//find our which velocity segment our enemy is at right now
			if(e.getVelocity()<-2){
				currentEnemyVelocity=0;
			}
			else if(e.getVelocity()>2){
				currentEnemyVelocity=1;
			}
			else if(e.getVelocity()<=2&&e.getVelocity()>=-2){
				if(currentEnemyVelocity==0){
					currentEnemyVelocity=2;
				}
				else if(currentEnemyVelocity==1){
						currentEnemyVelocity=3;
				}
			}
			
			//update the one we are using to determine where to store our velocities if we have fired and there has been enough time for a bullet to reach an enemy
			//(only a rough approximation of bullet travel time 12.8);
			if(getTime()-oldTime > e.getDistance()/12.8 && fired==true){
				aimingEnemyVelocity=currentEnemyVelocity;
			}
			else{
				fired=false;
			}
			
			//record a new enemy velocity and raise the count
			enemyVelocities[count][aimingEnemyVelocity]=e.getVelocity();
			count++;
			if(count==400){
				count=0;
			}
			
			//calculate our average velocity for our current segment
			averageCount=0;
			velocityToAimAt=0;
			while(averageCount<400){
				velocityToAimAt+=enemyVelocities[averageCount][currentEnemyVelocity];
				averageCount++;
			}
			velocityToAimAt/=400;
			
			
			//circular targeting code from Robowiki.
			//graphics that graph the enemies predicted movement 
			double bulletPower = Math.min(2.4,Math.min(e.getEnergy()/4,getEnergy()/10));
			double myX = getX();
			double myY = getY();
			double enemyX = getX() + e.getDistance() * Math.sin(absBearing);
			double enemyY = getY() + e.getDistance() * Math.cos(absBearing);
			double enemyHeading = e.getHeadingRadians();
			double enemyHeadingChange = enemyHeading - oldEnemyHeading; 
            oldEnemyHeading = enemyHeading;
			double deltaTime = 0;
			double battleFieldHeight = getBattleFieldHeight(), battleFieldWidth = getBattleFieldWidth();
			double predictedX = enemyX, predictedY = enemyY;

			while((++deltaTime) * (20.0 - 3.0 * bulletPower) < Point2D.Double.distance(myX, myY, predictedX, predictedY)){	//ensure the bullet will hit in time
				predictedX += Math.sin(enemyHeading) * velocityToAimAt;
				predictedY += Math.cos(enemyHeading) * velocityToAimAt;
				enemyHeading += enemyHeadingChange;

				g.setColor(Color.red);
				g.fillOval((int)predictedX-2,(int)predictedY-2,4,4);

				if(	predictedX < 18.0 || predictedY < 18.0|| predictedX > battleFieldWidth - 18.0 || predictedY > battleFieldHeight - 18.0){
					predictedX = Math.min(Math.max(18.0, predictedX), battleFieldWidth - 18.0);	
					predictedY = Math.min(Math.max(18.0, predictedY), battleFieldHeight - 18.0);
					break;
				}
			}
			double theta = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));

			setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians())*2);
			setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));

			if(getGunHeat()==0){
				fire(bulletPower);
				fired=true;
			}
 
		}
		
		
	}
}
