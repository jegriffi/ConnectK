import connectK.CKPlayer;
import connectK.BoardModel;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;


public class HolyMolyItsACannoliAI extends CKPlayer {
	public byte player;
	public byte otherPlayer;

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
		for (int y = height - 1; y >= 0; y--) {
			byte b = state.getSpace(0, y);
			if (b != 0) { // not empty square				
				if ((y+1) < height) {
					byte bU = state.getSpace(0, y+1);
					if (b == minPlayer) {  // is adversary						
						if (bU == minPlayer)  //if is also adversary
							board[0][y] = (byte)(((byte) -1) + board[0][y+1]);						
						else 
							board[0][y] = (byte) -1;
						minSum += board[0][y];
					}
					else { // is you
						if (bU == maxPlayer) 
							board[0][y] = (byte) (((byte) 1) + board[0][y+1]);
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
			for (int y = height - 1; y >= 0; y--) {
				byte b = state.getSpace(x-1, y);
				byte bU, bL, bD;
				board[1][y] = (byte) 0;
				if (b != 0) { //not empty					
					if ((y+1) < height) {
						bU = state.getSpace(x, y+1);
						bD = state.getSpace(x-1, y+1);
						if (b == minPlayer) {
							if (bU == minPlayer) 
								board[1][y] += (byte) (board[1][y+1]);
							if (bD == minPlayer)
								board[1][y] += (byte) (board[0][y+1]);							
						}
						else {
							if (bU == maxPlayer) 
								board[1][y] += (byte) (board[1][y+1]);
							if (bD == maxPlayer)
								board[1][y] += (byte)(board[0][y+1]);							
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
	
	private int consecutiveHelper(BoardModel state, int x, int y, byte player) {
		boolean breakflag = false;
		int playerOpportunity = 1, leftDiagonal = 1, rightDiagonal = 1, vertical = 1, horizontal = 1,
				opportunityVal = 5, currentSpace;
		
		//LUD
		for (int i = x-1; i >= 0; i--) {
			if (breakflag)
				break;
			for (int j = y+1; j < state.getHeight(); j++) {
				currentSpace = state.getSpace(i, j);
				if (currentSpace != player) {
					if (currentSpace == 0)  
						playerOpportunity += opportunityVal;
					breakflag = true;
					break;
				}
				if (currentSpace == player) {
					if (++leftDiagonal >= state.getkLength())
						return Integer.MAX_VALUE;
				}
			}
		}
		
		breakflag = false;
		//LLD
		for (int i = x-1; i >= 0; i--) {
			if (breakflag)
				break;
			for (int j = y-1; j > 0; j--) {
				currentSpace = state.getSpace(i, j);
				if (currentSpace != player) {
					if (currentSpace == 0) 
						playerOpportunity += opportunityVal;
					breakflag = true;
					break;
				}
				if (currentSpace == player) {
					if (++leftDiagonal >= state.getkLength())
						return Integer.MAX_VALUE;
				}
			}
		}
		
		breakflag = false;
		//RUD
		for (int i = x+1; i < state.getWidth(); i++) {
			if (breakflag)
				break;
			for (int j = y+1; j < state.getHeight(); j++) {
				currentSpace = state.getSpace(i, j);
				if (currentSpace != player) {
					if (currentSpace == 0) 
						playerOpportunity += opportunityVal;
					breakflag = true;
					break;
				}
				if (currentSpace == player) {
					if (++rightDiagonal >= state.getkLength())
						return Integer.MAX_VALUE;
				}
			}
		}
		
		breakflag = false;
		//RLD
		for (int i = x+1; i < state.getWidth(); i++) {
			if (breakflag)
				break;
			for (int j = y-1; j > 0; j--) {
				currentSpace = state.getSpace(i, j);
				if (currentSpace != player) {
					if (currentSpace == 0) 
						playerOpportunity += opportunityVal;
					breakflag = true;
					break;
				}
				if (currentSpace == player) {
					if (++rightDiagonal >= state.getkLength())
						return Integer.MAX_VALUE;
				}
			}
		}
		
		breakflag = false;
		//LH
		for (int i = x-1; i >= 0; i--) {
			int j = y;
			currentSpace = state.getSpace(i, j);
			if (currentSpace != player) {
				if (currentSpace == 0) 
					playerOpportunity += opportunityVal;
				break;
			}
			else {
				if (++horizontal >= state.getkLength()) 
					return Integer.MAX_VALUE;
			}			
		}
		
		//RH
		for (int i = x+1; i < state.getWidth(); i++) {
			int j = y;
			currentSpace = state.getSpace(i, j);
			if (currentSpace != player) {
				if (currentSpace == 0) 
					playerOpportunity += opportunityVal;
				break;
			}
			else {
				if (++horizontal >= state.getkLength()) 
					return Integer.MAX_VALUE;
			}			
		}
		
		//UV
		for (int j = y+1; j < state.getHeight(); j++) {
			int i = x;
			currentSpace = state.getSpace(i, j);
			if (currentSpace != player) {
				if (currentSpace == 0) 
					playerOpportunity += opportunityVal;
				break;
			}
			else {
				vertical++;
				if (vertical >= state.getkLength()) 
					return Integer.MAX_VALUE;
			}			
		}
		
		//LV
		for (int j = y-1; j >=0; j--) {
			int i = x;
			currentSpace = state.getSpace(i, j);
			if (currentSpace != player) {
				if (currentSpace == 0) 
					playerOpportunity += opportunityVal;
				break;
			}
			else {
				if (++vertical >= state.getkLength()) 
					return Integer.MAX_VALUE;
			}			
		}
		
		
		return (playerOpportunity + rightDiagonal + leftDiagonal + vertical + horizontal);
	}
	
	private int consecutiveTileHuristic(BoardModel state, byte player) {
		int sum = 0;
		for (int x = 0; x < state.getWidth(); x++) {
			for (int y = 0; y < state.getHeight(); y++) {
				sum += consecutiveHelper(state, x, y, player);
			}
		}
		return sum;
	}
	protected int alphaBetaPruningGravityOnMove(BoardModel state, int depth, int alpha, int beta, byte maxPlayer) {
		if (depth == 0 || !state.hasMovesLeft()) {
			int heuristic = consecutiveTileHuristic(state, maxPlayer);
			if (heuristic == Integer.MAX_VALUE || heuristic == Integer.MIN_VALUE)
				return heuristic;
			heuristic += DPHeuristic(state, maxPlayer);
			return heuristic;
		}
		List<Point> moveList = getListOfAllGravityOnMoves(state);
		
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
		Comparator<PointWithHeuristic> comparator = new HeuristicsComparator();
		PriorityQueue<PointWithHeuristic> pq = new PriorityQueue<PointWithHeuristic>(100, comparator);
		int depth = 0;
		if (state.gravityEnabled()) {
			depth = 4;
			moveList = getListOfAllGravityOnMoves(state);
		} else {
			depth = 2;
			moveList = getListOfAllGravityOffMoves(state, this.player);
		}
		int v;
		for (Point p : moveList) {			
			BoardModel tmpMove = state.placePiece(p, this.player);
			v = alphaBetaPruningGravityOnMove(tmpMove, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, this.otherPlayer);
			PointWithHeuristic pwh = new PointWithHeuristic(p, v);
			pq.add(pwh);
		}		
		
		return pq.remove().getPoint();
	}

	//Deadline checks the time left for you to make a move (something like that)
	@Override
	public Point getMove(BoardModel state, int deadline) {
		return getMove(state);
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
