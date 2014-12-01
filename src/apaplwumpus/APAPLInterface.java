package apaplwumpus;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.LinkedList; 
import javax.swing.JFrame;  
import base.Wumpus;
import base.GUI.WumpusGUI; 
import apapl.Environment;
import apapl.ExternalActionFailedException;
import apapl.data.APLFunction;
import apapl.data.APLIdent;
import apapl.data.APLList;
import apapl.data.APLNum;
import apapl.data.Term;
/**
 * The 2APL interface to the Wumpus world. This world supports up to 4 agents. The basic actions are moving, perceiving, gripping,
 * adding notices and showing the belief state. Do note that whatever the agent believes does not influence the state of the actual
 * world.
 * 
 * The graphical interface of the world can be switched off. This is highly recommended when experimenting with learning algorithms or
 * in other situations where performance is of importance. When the GUI is off, then the actions for showing beliefs and adding notices
 * will be ignored entirely and succeed always.
 * 
 * This interface creates a Wumpus world upon instantiation. It is not possible to connect the interface to an already initialized
 * world.
 * 
 * @author Bas Testerink
 */
public class APAPLInterface extends Environment {
	private Wumpus w = null;													// The real world
	private HashMap<String, Integer> agents = new HashMap<String, Integer>();	// Registered agents (max=4)
	private static Term YES = new APLIdent("yes"), NO = new APLIdent("no");
	boolean show_gui = true;													// Can turn of GUI
	WumpusGUI g = null; 														// The GUI
	
	/**
	 * Constructor. Here the Wumpus world and GUI are initialized as well.
	 */
	public APAPLInterface(){
		w = new Wumpus();														// Create reality
		w.loadFromFile("./resources/standard_wumpus.txt", true);				// Get the world specification
		if(show_gui){															// When a GUI is required
			g = new WumpusGUI(w, 30);											// Standard fps = 30, seems to work smoothly
			JFrame f = new JFrame();											// If you want to change the interface then you want to customize this frame
			g.setPreferredSize(new Dimension(720, 500));						// Fits nicely a 4x4 world
			f.add(g);	
			f.addWindowListener(new StopTheAnimation());
			f.pack();
			f.setVisible(true);
			g.start_animation();												// Starts the animation thread
		} 
	}
	
	/** 
	 * Small class to stop the animation thread when closing the frame. If you reload the MAS then you do not want stack 
	 * these threads.
	 * @author Bas Testerink, Utrecht University, The Netherlands
	 *
	 */
	class StopTheAnimation extends WindowAdapter {
		  public void windowClosing(WindowEvent evt){g.stop_animation();} 		// Stop the animation  
	}

	
	/**
	 * Register an agent. Only the first four agents get an id. For others actions will always fail.
	 */
	protected void addAgent(String agName) {
		if(agents.size()<5) agents.put(agName,agents.size()); 					// Max amount of agents is four, agents are identified by the order in which they entered
	}  
	
	/** Needed for compilation purposes. */
	public static void main(String [] args) { } 
	
	/**
	 * Move an agent to an adjacent tile.
	 * @param agName Name of the agent that performs the action.
	 * @param dir The direction must be one of: up, down, left or right.
	 * @return Always returns 'yes' upon success.
	 * @throws ExternalActionFailedException Fails when agent is not registered, the direction is illegal, or if the move was not possible.
	 */
	public Term move(String agName, APLIdent dir) throws ExternalActionFailedException {
		Integer agent = agents.get(agName);									// Get the registration number
		if(agent!=null){													// If the agent is registered
			if(dir.getName().equals("up")) w.move(agent, 0, 1);				// Try to move accordingly
			else if(dir.getName().equals("down")) w.move(agent, 0, -1);		// Note that y=0 is the bottom
			else if(dir.getName().equals("right")) w.move(agent, 1, 0);
			else if(dir.getName().equals("left")) w.move(agent, -1, 0);
			else throw new ExternalActionFailedException("Not a valid move direction: "+dir.getName()); // Illegal argument
			if(show_gui){
				try{Thread.sleep(600);}catch(Exception e){}					// Allow the animation to finish
				g.updateRealWorld();										// Update the real world drawing if needed
			}
			if(w.getFailedMessage()==null) return YES;						// If failed message != null then the action failed
			else throw new ExternalActionFailedException(w.getFailedMessage());
		} else  throw new ExternalActionFailedException("Agent not registered."); 
	}
	
	/**
	 * Try to use the gripper for picking up or dropping gold.
	 * @param agName Name of the agent that wants to drop/grab.
	 * @param type Either 'grab' or 'drop'. 
	 * @return Always returns 'yes' upon success.
	 * @throws ExternalActionFailedException Fails when the agent is not registered, the argument is illegal, or if grab/drop was inappropriate. 
	 */
	public Term gripper(String agName, APLIdent type) throws ExternalActionFailedException{ 
		Integer agent = agents.get(agName);									// Get the registration number
		if(agent!=null){													// If the agent is registered
			if(type.getName().equals("grab")) w.grab(agent);				// Try either grab...
			else if(type.getName().equals("drop")) w.drop(agent);			// ... or drop
			else throw new ExternalActionFailedException("Not a valid gripper action: "+type.getName()); // Illegal argument
			if(show_gui) g.updateRealWorld();								// Update the real world drawing if needed
			if(w.getFailedMessage()==null) return YES; 						// If failed message != null then the action failed
			else throw new ExternalActionFailedException(w.getFailedMessage()); 
		} else  throw new ExternalActionFailedException("Agent not registered."); 
	}
	
	/**
	 * Perceive the tile on which the performing agent stands.
	 * @param agName The agent that is perceiving. 
	 * @return A boolean tuple <Breeze,Glitter,Stench,Death> (Death stands for: agent has died) represented with a list. 
	 * @throws ExternalActionFailedException Fails when the agent was not registered.
	 */
	public Term perceive(String agName) throws ExternalActionFailedException{
		Integer agent = agents.get(agName);									// Get the registration number
		if(agent!=null){													// If the agent was registered
			int data = w.perceive(agent);									// Get the int with percepts
			LinkedList<Term> terms = new LinkedList<Term>(); 				// Initialize list arguments
			terms.add(new APLIdent((data&Wumpus.BREEZE)>0?"yes":"no"));		// Add boolean values
			terms.add(new APLIdent((data&Wumpus.GLITTER)>0?"yes":"no"));
			terms.add(new APLIdent((data&Wumpus.STENCH)>0?"yes":"no"));
			terms.add(new APLIdent(w.getDeaths()[agent]?"yes":"no"));
			return new APLList(terms);										// Build and return list
		} else  throw new ExternalActionFailedException("Agent not registered."); // Agent was not registered
		
	}
	
	/**
	 * For demonstration purposes one might want the agent to express its beliefs in the graphical interface. This action thus
	 * only affects the GUI by updating the agent's believed world. For the mood of the agent, use 0 for angry, 1 for content and 2
	 * for happy. Happy agents will be shown as having delivered gold since this is the Wumpus world's main purpose.
	 * @param agName Name of the agent that surrenders its beliefs to the GUI.
	 * @param world A list with information about the world and the agent's mood.
	 * @return Always returns 'yes'.
	 * @throws ExternalActionFailedException Fails when the agent is not registered.
	 */
	public Term surrender_beliefs(String agName, APLList world) throws ExternalActionFailedException { 
		if(show_gui){													// Ignored when there is no GUI
			Integer agent = agents.get(agName);							// Get the registration number
			if(agent!=null){											// If the agent was registered
				 g.clearBelievedWorld(agent); 							// Dispose of current believed world
				 while(!world.isEmpty()){								// For each element
					 APLFunction head = (APLFunction)world.getHead();	 
					if(head.getName().equals("mood")){					// The mood info, influences the agent's visual proxy
						if(((APLIdent)head.getParams().get(0)).getName().equals("angry")) g.updateMood(agent, 0);
						else if(((APLIdent)head.getParams().get(0)).getName().equals("content")) g.updateMood(agent, 1);
						else if(((APLIdent)head.getParams().get(0)).getName().equals("happy")) g.updateMood(agent, 2);
					} else {
						int x = ((APLNum)head.getParams().get(0)).toInt()-1; // Get the x and y of the object
						int y = ((APLNum)head.getParams().get(1)).toInt()-1;
						if(head.getName().equals("safe"))          g.addToBelievedWorld(agent, x, y, Wumpus.SAFE); // Add to world
						else if(head.getName().equals("wumpus"))   g.addToBelievedWorld(agent, x, y, Wumpus.WUMPUS);
						else if(head.getName().equals("stench"))   g.addToBelievedWorld(agent, x, y, Wumpus.STENCH);
						else if(head.getName().equals("pit"))      g.addToBelievedWorld(agent, x, y, Wumpus.PIT);
						else if(head.getName().equals("breeze"))   g.addToBelievedWorld(agent, x, y, Wumpus.BREEZE);
						else if(head.getName().equals("safe"))     g.addToBelievedWorld(agent, x, y, Wumpus.SAFE); 
						else if(head.getName().equals("visited"))  g.addToBelievedWorld(agent, x, y, -1); // Will remove unknown symbol
						else if(head.getName().equals("gold"))     g.addToBelievedWorld(agent, x, y, Wumpus.GOLD);  
						else if(head.getName().equals("chest"))    g.addToBelievedWorld(agent, x, y, Wumpus.CHEST);  
						else if(head.getName().equals("position")){ // Agent locations, note that agent sees itself always as blue (agent 0)
							if(head.getParams().size()==2) g.addToBelievedWorld(agent, x, y, 1<<(7+agent)); // Own position
							else g.addToBelievedWorld(agent, x, y, 1<<(7+((APLNum)head.getParams().get(2)).toInt()));
						}
					}
					world = (APLList)world.getTail(); // Continue with tail
				 }
			} else  throw new ExternalActionFailedException("Agent not registered.");  
		}
		return YES;
	}
	
	/**
	 * Add a small message on the notice board. Ideal for debugging as 2APL currently does not provide means for agents to
	 * output towards some private output stream. 
	 * @param agName Name of the agent that writes a message.
	 * @param topic The topic of the message. Can be used to overwrite older messages with the same topic.
	 * @param message The message itself. Note that there is not a lot of space so keep it short.
	 * @return Always returns 'yes'.
	 * @throws ExternalActionFailedException Fails when there is no more room for messages (max=11), or if agent was not registered.
	 */
	public Term addNote(String agName, APLIdent topic, APLIdent message) throws ExternalActionFailedException {
		if(show_gui){													// Ignored when there is no GUI
			Integer agent = agents.get(agName);							// Get the registration number
			if(agent!=null){											// If the agent was registered
				if(g.addNote(agent, topic.getName(), message.getName())) return YES; // Try the addition
				else throw new ExternalActionFailedException("Could not add note. Maximal amount is 11.");
			} else  throw new ExternalActionFailedException("Agent not registered."); 
		}
		return YES;
	}
}
