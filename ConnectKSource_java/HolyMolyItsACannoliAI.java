import connectK.CKPlayer;
import connectK.BoardModel;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class HolyMolyItsACannoliAI extends CKPlayer {
	public byte player;
	public byte otherPlayer;
	public long startTime;
	public long timeLimit;
	public long cushionTime = 1000;
	
	//for IDS
	public int CURRENT_HEURISTICS_SCORE = Integer.MIN_VALUE;
	public int DEPTH = 0;

	public HolyMolyItsACannoliAI(byte player, BoardModel state) {
		super(player, state);
		teamName = "HolyMolyItsACannoli";
		this.player = player;
		this.otherPlayer = getOtherPlayer();
	}
	
	private byte getOtherPlayer() {
		if (this.player == 1) 
			return (byte) 2;
		else
			return (byte) 1;
	}
	
	private boolean isValidGravityMove(BoardModel state, Point p) {
		int i = (int)p.getX();
		int j = (int)p.getY();
		if (state.getSpace(i, j) == 0) {
			if (j == 0 && state.getSpace(i, j) == 0) 
				return true;
			if ((j-1) >= 0 && state.getSpace(i, j-1) != 0) //something directly underneath
				return true;			
		}
		return false;		
	}
	
	private List<Point> getListOfAllGravityOnMoves(BoardModel state) {
		List<Point> moveList = new ArrayList<>();
		
		for(int x = 0; x < state.getWidth(); x++) {
			for (int y = 0; y < state.getHeight(); y++) {
				Point p = new Point(x, y);
				if (isValidGravityMove(state, p)) {
					moveList.add(p);
				}
			}
		}
		return moveList;
	}
	
	private List<Point> getListOfAllGravityOffMoves(BoardModel state, byte player) {
		List<Point> moveList = new ArrayList<>();
		
		for (int x = 0; x < state.getWidth(); x++) {
			for (int y = 0; y < state.getHeight(); y++) {
				if (state.getSpace(x, y) == 0) {
					moveList.add(new Point(x, y));
				}
			}
		}
		return moveList;
	}
	
	//DPHeuristic tries to focus on clustering the tiles together when the gravity is off.	
	//bU is the point above
	//bL is the point to left
	//bD is point diagonal
	private int DPHeuristic(BoardModel state, byte maxPlayer) {
		byte[][] board = new byte[2][state.getHeight()];
		int maxSum = 0, minSum = 0, width = state.getWidth(), height = state.getHeight();
		byte minPlayer = 0;
		if (maxPlayer == 1) 
			minPlayer = 2;
		else if (maxPlayer == 2) 
			minPlayer = 1;
		
		// initial column
		for (int y = 0; y < height; y++) {
			byte b = state.getSpace(0, y);
			if (b != 0) { // not empty square				
				if ((y-1) >= 0) {
					byte bU = state.getSpace(0, y-1);
					if (b == minPlayer) {  // is adversary						
						if (bU == minPlayer)  //if is also adversary
							board[0][y] = (byte)(((byte) -1) + board[0][y-1]);						
						else 
							board[0][y] = (byte) -1;
						minSum += board[0][y];
					}
					else { // is you
						if (bU == maxPlayer) 
							board[0][y] = (byte) (((byte) 1) + board[0][y-1]);
						else
							board[0][y] = (byte) 1;
						maxSum += board[0][y];
					}
				}
				else { //top of stack
					if (b == minPlayer)
						board[0][y] = -1;
					if (b == maxPlayer)
						board[0][y] = 1;
				}
			}
			else {
				board[0][y] = (byte) 0;
			}
		}
		
		//subsequent columns
		for (int x = 1; x < width; x++) {
			for (int y = 0; y < height; y++) {
				byte b = state.getSpace(x-1, y);
				byte bU, bL, bD;
				board[1][y] = (byte) 0;
				if (b != 0) { //not empty					
					if ((y-1) >= 0) {
						bU = state.getSpace(x, y-1);
						bD = state.getSpace(x-1, y-1);
						if (b == minPlayer) {
							if (bU == minPlayer) 
								board[1][y] += (byte) (board[1][y-1]);
							if (bD == minPlayer)
								board[1][y] += (byte) (board[0][y-1]);							
						}
						else {
							if (bU == maxPlayer) 
								board[1][y] += (byte) (board[1][y-1]);
							if (bD == maxPlayer)
								board[1][y] += (byte)(board[0][y-1]);							
						}
					} else {//add check if b is not empty
						if (b == minPlayer)
							board[1][y] = -1;
						if (b == maxPlayer)
							board[1][y] = 1;
					}
					bL = state.getSpace(x-1, y);
					if(b == minPlayer) {
						board[1][y] += (byte) -1;
						if (bL == minPlayer) 
							board[1][y] += (byte) (board[0][y]);
						minSum += board[1][y];
					}
					else {
						board[1][y] += (byte) 1;
						if (bL == maxPlayer)
							board[1][y] += (byte) (board[0][y]);	
						maxSum += board[1][y];
					}
				}
				// moves over unused elements to save on space
				if (y == 0) {
					board[0][y+2] = board[1][y+2];
					board[0][y+1] = board[1][y+1];
					board[0][y] = board[1][y];
				}
				else {
					if ((height - y) > 2) 
						board[0][y+2] = board[1][y+2];							
				}				
			}
		}
		
		return (maxSum-minSum);
	}
	
	//group of helper functions for main heuristics.
	private int leftUpperDiagonal(BoardModel state, int x, int y, byte player) {
		boolean breakflag = false;
		int currentSpace, leftDiagonal = 0;
		//LUD
		for (int i = x-1; i >= 0; i--) {
			if (breakflag)
				break;
			for (int j = y+1; j < state.getHeight(); j++) {
				currentSpace = state.getSpace(i, j);
				if (currentSpace != player) {
					breakflag = true;
					break;
				}
				if (currentSpace == player) {
					++leftDiagonal;
				}
			}
		}
		return leftDiagonal;
	}
	
	private int leftLowerDiagonal(BoardModel state, int x, int y, byte player) {
		//LLD
		boolean breakflag = false;
		int currentSpace, leftDiagonal = 0;
		for (int i = x-1; i >= 0; i--) {
			if (breakflag)
				break;
			for (int j = y-1; j > 0; j--) {
				currentSpace = state.getSpace(i, j);
				if (currentSpace != player) {
					breakflag = true;
					break;
				}
				if (currentSpace == player) {
					++leftDiagonal;
				}
			}
		}
		return leftDiagonal;
	}
	
	private int rightUpperDiagonal(BoardModel state, int x, int y, byte player) {
		//RUD
		boolean breakflag = false;
		int currentSpace, rightDiagonal = 0;
		for (int i = x+1; i < state.getWidth(); i++) {
			if (breakflag)
				break;
			for (int j = y+1; j < state.getHeight(); j++) {
				currentSpace = state.getSpace(i, j);
				if (currentSpace != player) {
					breakflag = true;
					break;
				}
				if (currentSpace == player) {
					++rightDiagonal;
				}
			}
		}
		return rightDiagonal;
	}
	
	private int rightLowerDiagonal(BoardModel state, int x, int y, byte player) {
		//RLD
		boolean breakflag = false;
		int currentSpace, rightDiagonal = 0;
		for (int i = x+1; i < state.getWidth(); i++) {
			if (breakflag)
				break;
			for (int j = y-1; j > 0; j--) {
				currentSpace = state.getSpace(i, j);
				if (currentSpace != player) {
					breakflag = true;
					break;
				}
				if (currentSpace == player) {
					++rightDiagonal;
				}
			}
		}
		return rightDiagonal;
	}
	
	private int leftHorizontal(BoardModel state, int x, int y, byte player) {
		//LH
		int currentSpace, horizontal = 0;
		for (int i = x-1; i >= 0; i--) {
			int j = y;
			currentSpace = state.getSpace(i, j);
			if (currentSpace != player) {
				break;					
			}
			else if (currentSpace == player) {
				horizontal++;
			}			
		}
		return horizontal;
	}
	
	private int rightHorizontal(BoardModel state, int x, int y, byte player) {
		//RH
		int currentSpace, horizontal = 0;
		for (int i = x+1; i < state.getWidth(); i++) {
			int j = y;
			currentSpace = state.getSpace(i, j);
			if (currentSpace != player) {
				horizontal = 0;
				break;				
			}
			else if (currentSpace == player) {
				++horizontal;
			}			
		}
		return horizontal;
	}
		
	private int upperVertical(BoardModel state, int x, int y, byte player) {
		//UV
		int currentSpace, vertical = 0;
		for (int j = y+1; j < state.getHeight(); j++) {
			int i = x;
			currentSpace = state.getSpace(i, j);
			if (currentSpace != player) {
				vertical = 0;
				break;		
			}
			else if (currentSpace == player) {
				vertical++;
			}			
		}
		return vertical;
	}
	
	private int lowerVertical(BoardModel state, int x, int y, byte player) {
		//LV
		int currentSpace, vertical = 0;
		for (int j = y-1; j >=0; j--) {
			int i = x;
			currentSpace = state.getSpace(i, j);
			if (currentSpace != player) {
				vertical = 0;
				break;
			}
			else if (currentSpace == player) {
				++vertical;
			}			
		}
		return vertical;
	}
	
	
	private int numberOfWins(BoardModel state, byte player) {
		List<Point> moveList;
		if (state.gravityEnabled())
			moveList = getListOfAllGravityOnMoves(state);
		else 
			moveList = getListOfAllGravityOffMoves(state, player);
		
		int LUD = 0, RUD = 0, LLD = 0, RLD = 0, LH = 0, RH = 0, UV = 0, LV = 0; 
		for (Point p : moveList) {
			LUD += leftUpperDiagonal(state, p.x, p.y, player);
			RUD += rightUpperDiagonal(state, p.x, p.y, player);
			LLD += leftLowerDiagonal(state, p.x, p.y, player);
			RLD += rightLowerDiagonal(state, p.x, p.y, player);
			LH += leftHorizontal(state, p.x, p.y, player);
			RH += rightHorizontal(state, p.x, p.y, player);
			UV += upperVertical(state, p.x, p.y, player);
			LV += lowerVertical(state, p.x, p.y, player);
		}
		int k = state.getkLength();
		int horizontalWins = (LH+RH) % k;
		int verticalWins = (UV+LV) % k;
		int leftDiagonalWins = (LUD+RLD) % k;
		int rightDiagonalWins = (RUD+LLD) % k;
		
		int playerBit = player == this.player ? 1 : -1;
		
		return playerBit*(horizontalWins+verticalWins+leftDiagonalWins+rightDiagonalWins);
	}
	
	
	/*
	 * this heuristic counts the difference of WINS between each player. if player has less wins, return MIN_VALUE
	 */
	public int baseCaseWinDiff(BoardModel state) { 
		int[] count = {0,0,0}; //[this Player - 0, Enemy - 1, Empty - 2]
		int playerWins = 0, enemyWins = 0;
		
		//ROWS
		for (int y = 0; y < state.getHeight(); y++) {
			for (int x = 0; x < state.getWidth(); x++) {
				if (state.getSpace(x, y) == 0) { //Empty; increment all 
					count[0] += 1;
					count[1] += 1;
					count[2] += 1;
				} else if (state.getSpace(x,y) == this.player) {
					count[0] += 1;
					count[1] = 0;
					count[2] = 0;
				} else { //Enemy player
					count[0] = 0;
					count[1] += 1;
					count[2] = 0;
				}				
				if ((count[0]+count[2]) / state.getkLength() >= 1) {
					count[0] -= 1;
					if (count[0] >= count[2])
						playerWins++;
					else 
						playerWins += 0.5;
				}
				if ((count[1]+count[2]) / state.getkLength() >= 1) {
					count[1] -= 1;
					if (count[1] >= count[2])
						enemyWins++;
					else enemyWins += 0.5;
				}
			}
		}
		
		//COLUMNS
		for (int x = 0; x < state.getWidth(); x++) {
			for (int y = 0; y < state.getHeight(); y++) {
				if (state.getSpace(x, y) == 0) { //Empty; increment all 
					count[0] += 1;
					count[1] += 1;
					count[2] += 1;
				} else if (state.getSpace(x,y) == this.player) {
					count[0] += 1;
					count[1] = 0;
					count[2] = 0;
				} else { //Enemy player
					count[0] = 0;
					count[1] += 1;
					count[2] = 0;
				}				
				if ((count[0]+count[2]) / state.getkLength() >= 1) {
					count[0] -= 1;
					if (count[0] >= count[2])
						playerWins++;
					else 
						playerWins += 0.5;
				}
				if ((count[1]+count[2]) / state.getkLength() >= 1) {
					count[1] -= 1;
					if (count[1] >= count[2])
						enemyWins++;
					else enemyWins += 0.5;
				}
			}
		}
		
		//LEFT DIAGONAL 
		for (int x = 0; x < state.getWidth(); x++) {
			int savedX = x, y = 0;
			while (savedX >= 0) {
				if (y>=state.getHeight()) {break;}
				if (state.getSpace(x, y) == 0) { //Empty; increment all 
					count[0] += 1;
					count[1] += 1;
					count[2] += 1;
				} else if (state.getSpace(x,y) == this.player) {
					count[0] += 1;
					count[1] = 0;
					count[2] = 0;
				} else { //Enemy player
					count[0] = 0;
					count[1] += 1;
					count[2] = 0;
				}				
				if ((count[0]+count[2]) / state.getkLength() >= 1) {
					count[0] -= 1;
					if (count[0] >= count[2])
						playerWins++;
					else 
						playerWins += 0.5;
				}
				if ((count[1]+count[2]) / state.getkLength() >= 1) {
					count[1] -= 1;
					if (count[1] >= count[2])
						enemyWins++;
					else enemyWins += 0.5;
				}
				
				savedX--;
				y++;
			}	
		}

		
		//LEFT DIAGONAL 2
		for (int y = 1; y < state.getHeight(); y++) {
			int tmpX = state.getHeight()-1, tmpY = y;
			while (tmpY < state.getHeight()) {
				if (tmpX < 0) {break;}
				if (state.getSpace(tmpX, tmpY) == 0) { //Empty; increment all 
					count[0] += 1;
					count[1] += 1;
					count[2] += 1;
				} else if (state.getSpace(tmpX,tmpY) == this.player) {
					count[0] += 1;
					count[1] = 0;
					count[2] = 0;
				} else { //Enemy player
					count[0] = 0;
					count[1] += 1;
					count[2] = 0;
				}				
				if ((count[0]+count[2]) / state.getkLength() >= 1) {
					count[0] -= 1;
					if (count[0] >= count[2])
						playerWins++;
					else 
						playerWins += 0.5;
				}
				if ((count[1]+count[2]) / state.getkLength() >= 1) {
					count[1] -= 1;
					if (count[1] >= count[2])
						enemyWins++;
					else enemyWins += 0.5;
				}
				
				tmpY++;
				tmpX--;
			}
		}
		
		
		//RIGHT DIAGONAL 1
		for (int y = (state.getHeight()-1); y >= 0; y--) {
			int x = 0, tmpY = y; 
			while (tmpY < state.getHeight()) {
				if (x>=state.getWidth()) {break;}
				if (state.getSpace(x, tmpY) == 0) { //Empty; increment all 
					count[0] += 1;
					count[1] += 1;
					count[2] += 1;
				} else if (state.getSpace(x,tmpY) == this.player) {
					count[0] += 1;
					count[1] = 0;
					count[2] = 0;
				} else { //Enemy player
					count[0] = 0;
					count[1] += 1;
					count[2] = 0;
				}				
				if ((count[0]+count[2]) / state.getkLength() >= 1) {
					count[0] -= 1;
					if (count[0] >= count[2])
						playerWins++;
					else 
						playerWins += 0.5;
				}
				if ((count[1]+count[2]) / state.getkLength() >= 1) {
					count[1] -= 1;
					if (count[1] >= count[2])
						enemyWins++;
					else enemyWins += 0.5;
				}
				
				tmpY++;
				x++;
			}			
		}
		
		//RIGHT DIAGONAL 2
		for (int x = 1; x < state.getWidth(); x++) {
			int y = 0, tmpX = x;
			while(tmpX < state.getWidth()) {
				if (y>=state.getHeight()) {break;}
				if (state.getSpace(tmpX, y) == 0) { //Empty; increment all 
					count[0] += 1;
					count[1] += 1;
					count[2] += 1;
				} else if (state.getSpace(tmpX,y) == this.player) {
					count[0] += 1;
					count[1] = 0;
					count[2] = 0;
				} else { //Enemy player
					count[0] = 0;
					count[1] += 1;
					count[2] = 0;
				}				
				if ((count[0]+count[2]) / state.getkLength() >= 1) {
					count[0] -= 1;
					if (count[0] >= count[2])
						playerWins++;
					else 
						playerWins += 0.5;
				}
				if ((count[1]+count[2]) / state.getkLength() >= 1) {
					count[1] -= 1;
					if (count[1] >= count[2])
						enemyWins++;
					else enemyWins += 0.5;
				}
				
				tmpX++;
				y++;
			}
		}
		
		if (playerWins <= enemyWins) {
			return Integer.MIN_VALUE;
		}
		return (playerWins);
	}
	
	public int Eval(BoardModel state, byte maxPlayer) {
		int heuristic = baseCaseWinDiff(state);
		if (!state.gravity) {				
			heuristic += DPHeuristic(state, maxPlayer);
		}
		
		heuristic += numberOfWins(state, maxPlayer); 
		return heuristic;
	}
	
	protected int alphaBetaPruningGravityOnMove(BoardModel state, int depth, int alpha, int beta, byte maxPlayer) {
		if (depth == 0 || !state.hasMovesLeft()) {		
			return Eval(state, maxPlayer);
		} 
		List<Point> moveList;
		if (state.gravityEnabled())
			moveList = getListOfAllGravityOnMoves(state);
		else 
			moveList = getListOfAllGravityOffMoves(state, maxPlayer);
		if (maxPlayer == this.player) {
			int v = Integer.MIN_VALUE;
			for (Point p : moveList) {
				BoardModel tmpMove = state.placePiece(p, this.player);
				v = Math.max(v, alphaBetaPruningGravityOnMove(tmpMove, depth - 1, alpha, beta, this.otherPlayer));
				alpha = Math.max(alpha, v);
				if (beta <= alpha)
					break;
			}
			return v;
		} else {
			int v = Integer.MAX_VALUE;
			for (Point p: moveList) {
				BoardModel tmpMove = state.placePiece(p, this.otherPlayer);
				v = Math.min(v, alphaBetaPruningGravityOnMove(tmpMove, depth-1, alpha, beta, this.player));
				beta = Math.min(beta, v);
				if (beta <= alpha) 
					break;
			}
			return v;
		}		
	}
	

	@Override
	public Point getMove(BoardModel state) {
		List<Point> moveList;

		if (state.gravityEnabled()) {
			if (state.lastMove == null) {
				return new Point(state.getWidth()/2, 0);
			}
			moveList = getListOfAllGravityOnMoves(state);
		} else {
			if (state.lastMove == null) {
				return new Point(state.getWidth()/2, state.getHeight()/2);
			}
			moveList = getListOfAllGravityOffMoves(state, this.player);
		}
		int v = Integer.MIN_VALUE;
		PointWithHeuristic maxPWH = new PointWithHeuristic(new Point(0, 0), Integer.MIN_VALUE);
		for (Point p : moveList) {		
			if (System.currentTimeMillis() - this.startTime > (this.timeLimit-this.cushionTime)) {break;}
			BoardModel tmpMove = state.placePiece(p, this.player);			
			v = alphaBetaPruningGravityOnMove(tmpMove, DEPTH, Integer.MIN_VALUE, Integer.MAX_VALUE, this.otherPlayer);
			PointWithHeuristic pwh = new PointWithHeuristic(p, v);
			if (v >= maxPWH.getHeuristic()) {
				maxPWH = pwh;
			}			
		}			
		CURRENT_HEURISTICS_SCORE = maxPWH.getHeuristic();
		return maxPWH.getPoint();
	}

	
	@Override
	public Point getMove(BoardModel state, int deadline) {
		Point point = null;
		this.cushionTime = 1500;
		this.timeLimit = deadline;
		this.startTime = System.currentTimeMillis();
		PointWithHeuristic maxPWH = new PointWithHeuristic(new Point(0, 0), Integer.MIN_VALUE);
		while (System.currentTimeMillis() - this.startTime < (this.timeLimit-this.cushionTime)) {
			Point move = getMove(state);
			int score = Integer.MIN_VALUE;
			if (CURRENT_HEURISTICS_SCORE >= maxPWH.getHeuristic()) {
				score = CURRENT_HEURISTICS_SCORE;
				maxPWH = new PointWithHeuristic(move, score);
			}
			DEPTH++;
		}
		DEPTH = 0;		
	    if (maxPWH.getHeuristic() == Integer.MIN_VALUE) {
	    	List<Point> moveList;
	    	if (state.gravityEnabled()) {
	    		moveList = getListOfAllGravityOnMoves(state);
	    	} else {
	    		moveList = getListOfAllGravityOffMoves(state, this.player);
	    	}		    		
	    	point = moveList.get(new Random().nextInt(moveList.size()-1));
	    } else {
	    	point = maxPWH.getPoint();
	    }
		
		return point;
	}
	
	//to be put into priorityQ
	private class PointWithHeuristic{
		private Point p;
		private int h;
		
		public PointWithHeuristic(Point p, int heuristic) {
			this.p = p;
			this.h = heuristic;
		}
		public Point getPoint() {
			return this.p;
		}
		public int getHeuristic() {
			return this.h;
		}
	}

	public class HeuristicsComparator implements Comparator<PointWithHeuristic> {
		
		@Override 
		public int compare(PointWithHeuristic x, PointWithHeuristic y) {
			if (x.getHeuristic() < y.getHeuristic()) {
				return -1;
			}
			if (x.getHeuristic() > y.getHeuristic()) {
				return 1;
			}
			return 0;
		}
	}
}
