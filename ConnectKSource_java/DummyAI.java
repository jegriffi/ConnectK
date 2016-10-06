import connectK.CKPlayer;
import connectK.BoardModel;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;


public class DummyAI extends CKPlayer {
	public byte player;

	public DummyAI(byte player, BoardModel state) {
		super(player, state);
		teamName = "HolyMolyItsACannoli";
		this.player = player;
	}
	
	protected Point minMaxGravityOnMove(BoardModel state, int depth, byte player) {
		if (depth == 0 || !state.hasMovesLeft()) {
			return null;
		}
				
		PriorityQueue<PointWithHeuristic> pq = getListOfAllGravityOnMoves(state, this.player);
		PointWithHeuristic pwh = pq.remove();
		return pwh.getPoint();
	}
	
	
	
	private boolean isValidGravityMove(BoardModel state, Point p) {
		int i = (int)p.getX();
		int j = (int)p.getY();
		if (state.getSpace(i, j) == 0) {
			if ((j-1) >= 0 && state.getSpace(i, j-1) != 0) //something directly underneath
				return true;				
		}
		return false;		
	}
	
	private PriorityQueue<PointWithHeuristic> getListOfAllGravityOnMoves(BoardModel state, byte player) {
		List<Point> points = new ArrayList<>();
		Comparator<PointWithHeuristic> comparator = new HeuristicsComparator();
		PriorityQueue<PointWithHeuristic> pq = new PriorityQueue<PointWithHeuristic>(100, comparator);
		
		//TODO switch to priority queue and pop them off incrementally
		for (int i = 0; i < state.getWidth(); i++) {
			for (int j = 0; j < state.getHeight(); j++) {
				if (isValidGravityMove(state, new Point(i, j))) {
					points.add(new Point(i, j));
					int h = surroundingTiles(state, player, new Point(i, j));
					pq.add(new PointWithHeuristic(new Point(i, j), h));
				}
			}
		}
		return pq;
	}
	
	private List<Point> getListOfAllNoGravityMoves(BoardModel state) {
		List<Point> points = new ArrayList<>();
		for (int i = 0; i < state.getWidth(); i++) {
			for (int j = 0; j < state.getHeight(); j++) {
				if (state.getSpace(i,j) == 0)
					points.add(new Point(i,j));
			}
		}
		return points;
	}

	@Override
	public Point getMove(BoardModel state) {
		return minMaxGravityOnMove(state, 1, player);
//		for(int i=0; i<state.getWidth(); ++i)
//			for(int j=0; j<state.getHeight(); ++j) {
//				if(state.getSpace(i, j) == 0) {
//					return new Point(i,j);
//				}
//			}
//
//		return null;
	}

	//Deadline checks the time left for you to make a move (something like that)
	@Override
	public Point getMove(BoardModel state, int deadline) {
		return getMove(state);
	}
	
	//basic heuristic
	public int surroundingTiles(BoardModel state, byte player, Point p) {
		int x = (int)p.getX(), y = (int)p.getY(), count = 0, 
				maxHeight = state.getHeight(), maxWidth = state.getWidth();
		for (int i = x-1; i < x+1; i++) {
			for (int j = y-1; j < y+1; j++) {
				if (i != x && j != y && i >= 0 && i < maxWidth && j >= 0 && j < maxHeight) {
					if (state.getSpace(i, j) == player)
						count++;
				}
			}
		}
		return count;
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
