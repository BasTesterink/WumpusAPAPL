package base.GUI;

import java.awt.Dimension;

import java.util.ArrayList;
import java.util.HashMap; 
import base.Wumpus;
/**
 * Instances of this class act as data containers. They hold a lot of arrays with Dimension objects that indicate where 
 * certain objects are positioned. 
 * 
 * @author Bas Testerink 
 *
 */
public class WumpusState {
	public Dimension[] agents = new Dimension[4];								// Positions where the agents should be
	public Dimension[] shown_agents = new Dimension[4];							// Positions where the agent currently are
	public ArrayList<Dimension> goldbars = new ArrayList<Dimension>();			// Various positions of objects
	public ArrayList<Dimension> breezes = new ArrayList<Dimension>();
	public ArrayList<Dimension> stenches = new ArrayList<Dimension>();
	public ArrayList<Dimension> wumpus = new ArrayList<Dimension>();
	public ArrayList<Dimension> filled_chests = new ArrayList<Dimension>();
	public ArrayList<Dimension> chests = new ArrayList<Dimension>(); 
	public ArrayList<Dimension> pits = new ArrayList<Dimension>();
	public ArrayList<Dimension> unknown = new ArrayList<Dimension>();
	public ArrayList<Dimension> safe = new ArrayList<Dimension>();
	public int agentnr = 0;														// The agent id whose world's representation this is
	public int[] moods = new int[]{0,0,0,0};									// Moods of the agents in this world
	public HashMap<String, Integer> labeled_notices = new HashMap<String, Integer>(); // Notice data
	ArrayList<String> notices = new ArrayList<String>(); 
	boolean[] has_gold = new boolean[]{false,false,false,false}, delivered_gold = new boolean[]{false,false,false,false}; // Who has/delivered gold
	
	/**
	 * Constructor. Places agents outside of this world.
	 */
	public WumpusState(){
		shown_agents = new Dimension[]{new Dimension(-50,-50),new Dimension(-50,-50),new Dimension(-50,-50),new Dimension(-50,-50)};
	}
	
	/**
	 * Update the positions of agents. They move towards where they should be.
	 */
	public void frame_call(){
		for(int i = 0; i < 4; i++)  														// For each agent
			if(agents[i]!=null){															// If it is in this world
				Dimension d = shown_agents[i];												// Get its shown (x,y)
				d.width = (int)(d.width + Math.ceil((agents[i].width-d.width)*0.2));		// Move it to where it should be
				d.height = (int)(d.height + Math.ceil((agents[i].height-d.height)*0.2));
			} 
	}
	
	/**
	 * Fill the world with "unknown" symbols.
	 * @param width Width of the world.
	 * @param height Height of the world.
	 * @param startX Starting pixel x of the world.
	 * @param startY Starting pixel y of the world.
	 * @param cell_size Pixel size of tiles (which are squares so one int is needed).
	 */
	public void fillUnknown(int width, int height, int startX, int startY, int cell_size){
		for(int x = 0; x < width; x++)
			for(int y = 0; y < height; y++){
				unknown.add(new Dimension(startX+x*cell_size, startY+y*cell_size));
			}
	}
	
	/**
	 * Load the world with data from a Wumpus state.
	 * @param w The Wumpus instance to represent.
	 * @param width Width of the world.
	 * @param height Height of the world.
	 * @param startX Starting pixel x of the world.
	 * @param startY Starting pixel y of the world.
	 * @param cell_size Pixel size of tiles (which are squares so one int is needed).
	 */
	public void fillRealWorld(Wumpus w, int width, int height, int startX, int startY, int cell_size){
		clearSubjective();																	// Clear the memory
		for(int x = 0; x < width; x++)														// For each cell
			for(int y = 0; y < height; y++){
				int pix_x = startX+x*cell_size;												// Get its pixel x and y
				int pix_y = startY+(height-y-1)*cell_size;
				if(w.at(x, y, Wumpus.CHEST)&&w.at(x, y, Wumpus.GOLD)) filled_chests.add(new Dimension(pix_x,pix_y)); // Filled chest if gold is on a chest
				else if(w.at(x, y, Wumpus.CHEST)) chests.add(new Dimension(pix_x,pix_y));	// Add the various possible symbols
				else if(w.at(x, y, Wumpus.GOLD)) goldbars.add(new Dimension(pix_x,pix_y));
				if(w.at(x, y, Wumpus.BREEZE)) breezes.add(new Dimension(pix_x,pix_y));
				if(w.at(x, y, Wumpus.PIT)) pits.add(new Dimension(pix_x,pix_y));	
				if(w.at(x, y, Wumpus.STENCH)) stenches.add(new Dimension(pix_x,pix_y));
				if(w.at(x, y, Wumpus.WUMPUS)) wumpus.add(new Dimension(pix_x,pix_y));
				if(w.at(x, y, Wumpus.AGENT0)) agents[0] = new Dimension(pix_x,pix_y); 
				if(w.at(x, y, Wumpus.AGENT1)) agents[1] = new Dimension(pix_x,pix_y); 
				if(w.at(x, y, Wumpus.AGENT2)) agents[2] = new Dimension(pix_x,pix_y); 
				if(w.at(x, y, Wumpus.AGENT3)) agents[3] = new Dimension(pix_x,pix_y); 
			}
	}
	
	/** Reset part of the data. */
	public void clearSubjective(){
		agents = new Dimension[4];
		goldbars.clear();
		breezes.clear();
		stenches.clear();
		wumpus.clear();
		chests.clear();
		filled_chests.clear();
		pits.clear();
		unknown.clear();
		safe.clear();
		has_gold = new boolean[]{false,false,false,false};
		delivered_gold = new boolean[]{false,false,false,false}; 
	}
	
	/**
	 * Add an object to this world. Will remove the unknown symbol if one is present at the objects location.
	 * @param x x position of the object.
	 * @param y y position of the object.
	 * @param obj The object to be added.
	 */
	public void add(int x, int y, int obj){
		Dimension d = new Dimension(x,y); 
		searchAndTransfer(d, unknown, null); 			// Remove the unknown symbol
		if(obj==Wumpus.PIT) pits.add(d);				// Add object to the appropriate array
		else if(obj==Wumpus.BREEZE) breezes.add(d);
		else if(obj==Wumpus.STENCH) stenches.add(d);
		else if(obj==Wumpus.WUMPUS) wumpus.add(d);
		else if(obj==Wumpus.CHEST) chests.add(d);
		else if(obj==Wumpus.AGENT0) agents[0] = d;
		else if(obj==Wumpus.AGENT1) agents[1] = d;
		else if(obj==Wumpus.AGENT2) agents[2] = d;
		else if(obj==Wumpus.AGENT3) agents[3] = d;
		else if(obj==Wumpus.SAFE) safe.add(d);
		else if(obj==Wumpus.CHEST){ 					// When a chest and goldbar are on the same spot, show a filled chest
			if(goldbars.contains(d)) searchAndTransfer(d, goldbars, filled_chests); // Check for open chest
			else chests.add(d);
		} else if(obj==Wumpus.GOLD){
			if(chests.contains(d)) searchAndTransfer(d, chests, filled_chests);     // Check for open chest
			else goldbars.add(d);
		}
		
	}
	
	/**
	 * Search for an instance of Dimension in one array, and if it exists remove it and put it in another.
	 * @param d Dimension to searched and transferred.
	 * @param ar1 Array from which to remove.
	 * @param ar2 Array in which to add.
	 */
	private void searchAndTransfer(Dimension d, ArrayList<Dimension> ar1, ArrayList<Dimension> ar2){
		for(int i = 0; i < ar1.size(); i++)
			if(ar1.get(i).equals(d)){
				ar1.remove(i);
				if(ar2!=null) ar2.add(d);
				break;
			} 
	}
}
