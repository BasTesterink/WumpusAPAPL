package base; 

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader; 
import javax.swing.JFileChooser; 
/**
 * This class implements the Wumpus world for maximally four agents. Creating Wumpus agents is an excellent way of practicing
 * (symbolic) A.I. as it requires some reasoning and path planning. A standard, solvable world is:
 * dimensions 4 4
 * agent 0 0 0
 * wumpus 0 2
 * pit 2 0
 * pit 2 2
 * pit 3 3
 * chest 0 0
 * gold 1 2
 * Copy paste the above specification in a file and use <code>loadFromFile</code> to load it. In this simplified version we do
 * not allow agents to shoot an arrow. 
 * 
 * The basic idea of the data layout is that the world is a double array of integers. Each integer is treated as an array of 32 bits.
 * Objects (agents, pits, etc) have a number that equals a bit position. One can thus add/remove an object by adding/subtracting it from
 * a cell's integer. This style allows for small memory usage and is quite fast, which is ideal if one wants to transfer the world to
 * a device with little computational power. 
 * 
 * @author Bas Testerink
 *
 */
public class Wumpus {
	public static final int GLITTER=1,BREEZE=1<<1,STENCH=1<<2,PIT=1<<3,GOLD=1<<4,CHEST=1<<5,WUMPUS=1<<6,AGENT0=1<<7,AGENT1=1<<8,AGENT2=1<<9,AGENT3=1<<10,UNKNOWN=1<<11,SAFE=1<<12; // Possible entities
	private String failed_message = null, lastfile=null;   	// Message when an action fails
	private int[][] world = null; 	      					// The world
	private int[] agent_locations = null; 					//(x,y) coordinates of agents
	private boolean[] is_dead, has_gold;  					// Keep track of whether agents are dead and whether they own gold
	private int width,height;			  					// World's dimensions

	/** Constructor. Calls <code>reset()</code> to initialize the data. */
	public Wumpus() { reset(); }
	
	/**
	 * Load a file. Must start with the world's dimensions. Agents have an extra argument in the end which indicates the number
	 * of the agent. The agent's number must be between 0 and 3 (inclusive).
	 * @param file File to load.
	 * @param give_choice When set to true a file chooser will appear for selecting a file.
	 */
	public void loadFromFile(String file, boolean give_choice){
		try {
			File f = new File(file);
			if(give_choice){ 														// If we want the user to choose a file
				JFileChooser jfc = new JFileChooser(".");							// Make file chooser in root map
				jfc.setDialogTitle("Choose Wumpus file");
				jfc.setSelectedFile(f); 											// Set the given selected file
				if(jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) 		// Get the user's choice
					f = jfc.getSelectedFile();
			}
			BufferedReader reader = new BufferedReader(new FileReader(f)); 			// Make reader
			String[] dimension = reader.readLine().split(" ");				   		// First line: Dimension: width height
			width = Integer.parseInt(dimension[1]);  								// Get the world's dimensions
			height = Integer.parseInt(dimension[2]);
			world = new int[width][height];			 								// Initiate world
			String s = reader.readLine();
			while(s!=null){							 								// Read through file 
				String[] split = s.split(" "); 		 								// Split the string
				int x = Integer.parseInt(split[1]);  								// Get x and y
				int y = Integer.parseInt(split[2]);
				if(split[0].equals("gold")){		 			  					// gold x y
					world[x][y] += GLITTER;						  					// Place glitter
					world[x][y] += GOLD;						  					// Place gold
				} else if(split[0].equals("chest")) world[x][y] += CHEST;			// chest x y
				else if(split[0].equals("pit")){		 		  					// pit x y
					world[x][y] += PIT;						  	 					// Place pit
					if(!at(x-1,y,BREEZE)&&!outside(x-1,y))world[x-1][y] += BREEZE;  // Add breezes
					if(!at(x+1,y,BREEZE)&&!outside(x+1,y))world[x+1][y] += BREEZE;
					if(!at(x,y-1,BREEZE)&&!outside(x,y-1))world[x][y-1] += BREEZE;
					if(!at(x,y+1,BREEZE)&&!outside(x,y+1))world[x][y+1] += BREEZE;
				} else if(split[0].equals("wumpus")){		 	  					// wumpus x y
					world[x][y] += WUMPUS;						  					// Place wumpus
					if(!at(x-1,y,STENCH)&&!outside(x-1,y))world[x-1][y] += STENCH; 	// Add stench
					if(!at(x+1,y,STENCH)&&!outside(x+1,y))world[x+1][y] += STENCH;
					if(!at(x,y-1,STENCH)&&!outside(x,y-1))world[x][y-1] += STENCH;
					if(!at(x,y+1,STENCH)&&!outside(x,y+1))world[x][y+1] += STENCH;
				} else if(split[0].equals("agent")){			  					// agent x y agentnr
					int agentnr = Integer.parseInt(split[3]);						// Get agent's id number
					world[x][y] += (1<<(7+agentnr));							 	// Add agent to the world
					agent_locations[agentnr*2] = x;									// Remember its location (for the actions later on)
					agent_locations[agentnr*2+1] = y;
				}
				s = reader.readLine();												// Go to next line
			}
		} catch(Exception e){e.printStackTrace();}
	}

	/**
	 * Check whether a certain object is present at some x and y in the world.
	 * @param x X coordinate.
	 * @param y Y coordinate.
	 * @param object The object which is being checked.
	 * @return Returns true if the object is present at (x,y), false otherwise. 
	 */
	public boolean at(int x, int y, int object){
		if(outside(x,y)) return false;		// Outside of the world is nothing
		else return (world[x][y]&object)>0; // Check for the presence of the object
	}
	
	/**
	 * Check whether a (x,y)-coordinate falls outside of the world.
	 * @param x X coordinate.
	 * @param y Y coordinate.
	 * @return Returns true if the x and y are smaller than 0 or bigger than width/height respectively.
	 */
	public boolean outside(int x, int y){
		return x<0||y<0||x>=width||y>=height; // perform the check
	}

	// Some getters.
	public String getFailedMessage(){ return failed_message; }
	public int[][] getWorld(){ return world; }
	public boolean[] getDeaths(){ return is_dead; }
	public boolean hasGold(int agent){ return has_gold[agent]; }

	/**
	 * Reset the world, also reloads the latest file.
	 */
	public void reset(){
		agent_locations = new int[8];
		is_dead = new boolean[]{false,false,false,false};
		has_gold = new boolean[]{false,false,false,false};
		if(lastfile!=null) loadFromFile(lastfile,false);
	}
	
	/*
	 * AGENT ACTIONS 
	 * Actions reset the fail message to null. If an action failed, then it will set the message to the reason why. 
	 * An outside interface can check whether an action succeeded by performing the check getFailedMessage==null
	 */
	/**
	 * Move an agent. Fails if the agent would end up outside of the world, or if the agent is already dead.
	 * Note that in principle it is allowed to move more than one square and towards any position in the world. It is up
	 * to the system that uses this world whether to restrict movement.
	 * @param agent The agent that moves (between 0 and 3, inclusive)
	 * @param dX Delta x, the amount of movement horizontally.
	 * @param dY Delta y, the amount of movement vertically.
	 */
	public void move(int agent, int dX, int dY){
		failed_message = null; 										// Reset fail message
		if(!is_dead[agent]){ 										// No zombies allowed
			int oldX = agent_locations[agent*2];					// Get old x and y
			int oldY = agent_locations[agent*2+1];
			int agentX = oldX+dX;									// Get new x and y
			int agentY = oldY+dY;
			if(agentX<0||agentX>=width||agentY<0||agentY>=height) 	// Agents may not move outside of the world
				failed_message = "Agent "+agent+" could not move with dX = "+dX+" and dY = "+dY+".";
			else {
				agent_locations[agent*2] = agentX;  				// Update location
				agent_locations[agent*2+1] = agentY;
				world[oldX][oldY] -= (1<<(7+agent));				// Remove from old position
				world[agentX][agentY] += (1<<(7+agent));			// Add to new position
				if(at(agentX,agentY,PIT)||at(agentX,agentY,WUMPUS)) // Check whether the agent is now dead after the movement
					is_dead[agent] = true;
			}
		} else failed_message = "Dead agent "+agent+" is  trying  to move.";
	}

	/**
	 * Get the world data of the position the agent is standing on. Returns the int representation with 1st bit: glitter,
	 * 2nd bit: breeze, 3rd bit: stench, 4th bit: gold, 5th bit: chest, 6th bit: wumpus, 7/8/9/10th bits: agents, 11th bit: unknown
	 * 12th bit: is safe. Perceiving fails if the agent is dead (and then always returns 0, it can't perceive anything after all).
	 * @param agent The perceiving agent.
	 * @return Integer that holds the position's data or 0 if agent is dead.
	 */
	public int perceive(int agent){
		failed_message = null; 						// Reset fail message
		if(!is_dead[agent]){ 						// No zombies allowed
			int agentX = agent_locations[agent*2];	// Get agent's x and y
			int agentY = agent_locations[agent*2+1]; 
			return world[agentX][agentY]; 			// Agents perceive the spot they stand on 
		} else failed_message = "Dead agent "+agent+" is  trying  to perceive.";
		return 0;									// Return 0 when not alive
	}

	/**
	 * Try to grab the gold. Fails if the agent is dead, already has gold, or is not standing on gold. 
	 * @param agent The agent that wants to grab.
	 */
	public void grab(int agent){ 				 	// Reset fail message
		failed_message = null;
		if(!is_dead[agent]&&!has_gold[agent]){ 		// Must be alive and have free hands
			int agentX = agent_locations[agent*2];	// Get agent's x and y
			int agentY = agent_locations[agent*2+1]; 
			if(at(agentX,agentY,GOLD)){				// Check whether there is gold to grab
				world[agentX][agentY] -= GOLD;		// Remove the gold
				world[agentX][agentY] -= GLITTER;   // Glitter at the location is now also removed
				has_gold[agent] = true; 			// Update has_gold fact
			} else failed_message = "Agent "+agent+" grabs but there is no gold.";
		} else failed_message = "Agent "+agent+" is either dead or already has gold while trying to grab.";
	} 

	/**
	 * Try to drop the gold. Fails if the agent is dead, has no gold or is standing on gold.
	 * @param agent The agent that wants to drop gold.
	 */
	public void drop(int agent){
		failed_message = null; 						// Reset fail message
		if(!is_dead[agent]&&has_gold[agent]){ 		// Must be alive and have gold
			int agentX = agent_locations[agent*2];	// Get agent's x and y
			int agentY = agent_locations[agent*2+1]; 
			if(!at(agentX,agentY,GOLD)){			// Cannot drop gold on gold
				world[agentX][agentY] += GOLD;		// Add the gold
				world[agentX][agentY] += GLITTER;	// Add corresponding glittering
				has_gold[agent] = false; 			// Update has_gold fact
			} else failed_message = "Agent "+agent+" drops but there is already gold.";
		} else failed_message = "Agent "+agent+" is either dead or has no gold while trying to drop.";
	}
}