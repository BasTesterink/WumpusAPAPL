package base.GUI;
 
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList; 
import javax.imageio.ImageIO; 
import javax.swing.JFileChooser;
import javax.swing.JPanel;  
import base.Wumpus;

/**
 * This class implements a graphical interface for the Wumpus world. The used images are provided in the distribution archive and
 * are free for non-commercial use. See the icon directory for links towards websites where I found them and where you can get licenses.
 * Currently this class holds no means to switch between agents. The projects first priority was to make everything ready for a single
 * agent Wumpus system.
 * 
 * To draw the Wumpus world we keep different WumpusState objects. These objects are only used for storing data for drawing and are not
 * related to the Wumpus instantiation which is used for determining the outcome of actions.
 * 
 * As you might note this class extends JPanel so it can be fitted inside another GUI. To actually see it you have to create a JFrame
 * or something similar and put this panel on it.
 * 
 * @author Bas Testerink 
 *
 */
public class WumpusGUI extends JPanel { 
	private Wumpus current_state = null; 										// The Wumpus instantiation on which agent operate
	private FrameCaller frame_caller = null;									// Runnable that can update agent positions
	private Thread frame_caller_thread = null;									// Thread that holds the runnable
	private int width,height,cell_size, selected_agent=0;						// World width/height/cell_size and the selected agent
	private BufferedImage[] numbers = null, chests = null;						// Border numbers and chest states
	private BufferedImage[][] agent_status_options = null, small_agent = null;	// Agent states
	private BufferedImage reset = null, notice_board = null, bigchest = null,	// Some icons
						  breeze = null, stench = null, wumpus = null, pit = null, unknown = null,
						  safe = null, gold = null, grabbed_gold = null;
	private String icon_dir = "./resources/icons/";								// Directory that holds the icons
	private Font font = new Font( Font.SANS_SERIF,Font.BOLD, 20), small_font = new Font(Font.SANS_SERIF,Font.PLAIN, 10); // Fonts for titles and notice board
	private WumpusState[] believed_worlds = null;								// Believed worlds of the agents
	private WumpusState real_world = null; 										// Representation of reality
	
	/**
	 * Constructor. Loads all the images.
	 * @param wumpus The Wumpus world of which this is the GUI.
	 * @param fps The frame rate per second for updating the position of agents.
	 */
	public WumpusGUI(Wumpus wumpus, int fps){
		File f = new File(icon_dir);
		if(!f.exists()){													// If the icon map does not exist
			JFileChooser jfc = new JFileChooser(".");						// Make file chooser in root map
			jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);		// Directory only
			jfc.setAcceptAllFileFilterUsed(false);
			jfc.setDialogTitle("Choose icon directory");
			jfc.setSelectedFile(f); 										// Set the given selected file
			if(jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) 	// Get the user's choice
				icon_dir = jfc.getSelectedFile().getPath()+"/"; 			// Set the directory with the images
		}
		current_state = wumpus;						
		width = wumpus.getWorld().length;									// Get width and height of the world
		height = wumpus.getWorld()[0].length;
		cell_size = 48;
		numbers = new BufferedImage[Math.max(height,width)];				// Init border number's array
		for(int i = 1; i <= numbers.length; i++) numbers[i-1] = getIcon(icon_dir+i+".png"); // Load the border numbers
		notice_board = getIcon(icon_dir+"log.png");							// Get the different icons
		reset = getIcon(icon_dir+"reset.png");
		bigchest = getIcon(icon_dir+"filledlarge.png");
		breeze = getIcon(icon_dir+"breeze.png");
		stench = getIcon(icon_dir+"stench.png");
		this.wumpus = getIcon(icon_dir+"wumpus.png");
		pit = getIcon(icon_dir+"pit.png");
		unknown = getIcon(icon_dir+"unknown.png");
		safe = getIcon(icon_dir+"safe.png");
		gold = getIcon(icon_dir+"gold.png"); 
		grabbed_gold = getIcon(icon_dir+"grabbed_gold.png"); 
		agent_status_options = new BufferedImage[][]						// Get the agent icons, 0=blue,1=red,2=yellow,3=green
				{{getIcon(icon_dir+"Bangrylarge.png"),getIcon(icon_dir+"Bcontentlarge.png"),getIcon(icon_dir+"Bhappylarge.png")},
				 {getIcon(icon_dir+"Rangrylarge.png"),getIcon(icon_dir+"Rcontentlarge.png"),getIcon(icon_dir+"Rhappylarge.png")}, 
				 {getIcon(icon_dir+"Yangrylarge.png"),getIcon(icon_dir+"Ycontentlarge.png"),getIcon(icon_dir+"Yhappylarge.png")},
				 {getIcon(icon_dir+"Gangrylarge.png"),getIcon(icon_dir+"Gcontentlarge.png"),getIcon(icon_dir+"Ghappylarge.png")}};
		small_agent = new BufferedImage[][]									// Small agent icons
				{{getIcon(icon_dir+"Bangry.png"),getIcon(icon_dir+"Bcontent.png"),getIcon(icon_dir+"Bhappy.png")},
				 {getIcon(icon_dir+"Rangry.png"),getIcon(icon_dir+"Rcontent.png"),getIcon(icon_dir+"Rhappy.png")}, 
				 {getIcon(icon_dir+"Yangry.png"),getIcon(icon_dir+"Ycontent.png"),getIcon(icon_dir+"Yhappy.png")},
				 {getIcon(icon_dir+"Gangry.png"),getIcon(icon_dir+"Gcontent.png"),getIcon(icon_dir+"Ghappy.png")}};
		chests = new BufferedImage[]{getIcon(icon_dir+"treasure.png"),getIcon(icon_dir+"filled.png")};
		believed_worlds = new WumpusState[4];								// Initialize believed worlds
		for(int i = 0; i < 4; i++){											// Create instance and fill with the unknown
			believed_worlds[i] = new WumpusState();
			believed_worlds[i].agentnr = i;
			believed_worlds[i].fillUnknown(width, height, (width+2)*cell_size, cell_size, cell_size); 
		}
		real_world = new WumpusState();										// Initialize the real world
		real_world.fillRealWorld(wumpus, width, height, cell_size, cell_size, cell_size);
		frame_caller = new FrameCaller(this, fps);							// Create update runnable
	}
	
	/** Start updates and animation */
	public void start_animation(){
		frame_caller.setHalt(false);					// Will prevent the run-loop from halting
		frame_caller_thread = new Thread(frame_caller); // Create new thread
		frame_caller_thread.start();					// Start it
	}
	
	/** Stop updates and animation */
	public void stop_animation(){
		frame_caller.setHalt(true); 					// Will stop the current run-loop
	}
	
	/** Calls the world states to update the position of agents. */
	public void frame_update(){
		real_world.frame_call();// Update all positions
		for(int i = 0; i < 4; i++) believed_worlds[i].frame_call();
	}
	
	/** Reload the Wumpus state into its visual equivalent and paint it. */
	public void updateRealWorld(){
		real_world.fillRealWorld(current_state, width, height, cell_size, cell_size, cell_size);
		repaint();
	}
	
	/**
	 * Remove all data of the visual world of an agent.
	 * @param agent The agent of which the belief is cleared.
	 */
	public void clearBelievedWorld(int agent){
		believed_worlds[agent].clearSubjective();
		believed_worlds[agent].fillUnknown(width, height, (width+2)*cell_size, cell_size, cell_size);
	}
	
	/**
	 * Add an object to an agents believed world representation.
	 * @param agent The id of the agent whose believed world is modified.
	 * @param x x position of the object.
	 * @param y y position of the object.
	 * @param obj The object to be added, use constants from the Wumpus class, such as Wumpus.GOLD. 
	 */
	public void addToBelievedWorld(int agent, int x, int y, int obj){
		believed_worlds[agent].add((width+2+x)*cell_size, (height-y)*cell_size, obj); 
	}
	
	/**
	 * Update the mood of an agent.
	 * @param agent The agent whose mood is changed.
	 * @param mood Use 0 for angry, 1 for content, 2 for happy.
	 */
	public void updateMood(int agent, int mood){
		believed_worlds[agent].moods[agent] = mood; // Change the mood in the believed world
		real_world.moods[agent] = mood; 			// But also in the real world (for the correct visual proxy icon)
	}
	
	/**
	 * Add a note on the notice board. These are personal for agents. Maximum amount of messages is 11.
	 * @param agent Id of the agent that writes a messages.
	 * @param topic Topic of the message (messages with the same topic are overwritten).
	 * @param msg Message itself.
	 * @return Returns true if the message was succesfully added.
	 */
	public boolean addNote(int agent, String topic, String msg){
		WumpusState s = believed_worlds[agent]; 					// Get the believed world
		synchronized(s.notices){									// Synchronize to prevent ConcurrentModExceptions
			if(s.labeled_notices.get(topic)==null){					// First message on the topic
				if(s.notices.size()==11) return false;				// Check if maximum is reached
				s.labeled_notices.put(topic, s.notices.size());		// Add to data
				s.notices.add("");
			}
			s.notices.remove((int)s.labeled_notices.get(topic));	// Remove previous message on this topic
			s.notices.add((int)s.labeled_notices.get(topic),msg);	// Add new one
		}
		return true;
	}
	
	/**
	 * Paints the real world and the believed world of the selected agent.
	 */
	public void paint(Graphics graphics){
		Graphics2D g = (Graphics2D)graphics;
		g.setBackground(Color.white); 
		g.clearRect(0, 0,getWidth(), getHeight()); 												// Clear the panel
		g.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING,					// Anti-alias looks pretty
				RenderingHints.VALUE_ANTIALIAS_ON));
		
		// Draw lines
		for(int y = 1; y <= height+1; y++){
			g.drawLine(cell_size, y*cell_size, (width+1)*cell_size, y*cell_size); 			  	// Real world lines
			g.drawLine(cell_size*(width+2), y*cell_size, (2*width+2)*cell_size, y*cell_size); 	// Believed world lines
		}
		for(int x = 1; x <= width+1; x++){
			g.drawLine(x*cell_size,cell_size,x*cell_size,(height+1)*cell_size);						// Real world lines
			g.drawLine((x+width+1)*cell_size,cell_size,(x+width+1)*cell_size,(height+1)*cell_size); // Believed world lines
		}
		g.drawRect(cell_size, cell_size, cell_size * (width*2+1), cell_size * (height+1));	  	// Border rectangle
		
		// Draw numbers
		for(int y = 0; y < height; y++) 														// The vertical numbers
			g.drawImage(numbers[y], cell_size*(width+1), (height-y)*cell_size, numbers[y].getWidth(), numbers[y].getHeight(), null);
		for(int x = 0; x < width; x++){ 														// The horizontal numbers
			g.drawImage(numbers[x], (x+1)*cell_size, (height+1)*cell_size, numbers[x].getWidth(), numbers[x].getHeight(), null);
			g.drawImage(numbers[x], (width+x+2)*cell_size, (height+1)*cell_size, numbers[x].getWidth(), numbers[x].getHeight(), null);
		}
		
		// Draw titles
		g.setFont(font);
		g.drawString("Real world:", cell_size, cell_size-3);
		g.drawString("Believed world:", cell_size*(width+2), cell_size-3);
		g.drawString("Notices:", cell_size*(2*width+3), cell_size-3);
		g.drawString("Status of agent "+selected_agent+":", cell_size, (height+3)*cell_size);
		
		// Noticeboard and agent status
		g.drawImage(notice_board,cell_size*(2*width+2), cell_size,null);     					// Wooden notice board
		//g.drawImage(reset,cell_size*(2*width), (height+3)*cell_size,null);   // Yellow reset button in 2apl not needed as it has its own reset button
		
		// Draw the world states
		drawWumpusState(g, believed_worlds[selected_agent], true);								// World of the selected agent
		drawWumpusState(g, real_world, false);													// Real world representation
	} 
	
	/**
	 * Draw the visual representation of a Wumpus world. If the world is a believed world from an agent, then it will also draw the 
	 * big status icon. The GUI adds a grabbed gold if the agent is content and a filled treasure chest if it is happy.
	 * @param g The object to draw upon.
	 * @param state The state to draw.
	 * @param subjective True if the state is a belief state of an agent, false otherwise.
	 */
	public void drawWumpusState(Graphics2D g, WumpusState state, boolean subjective){
		drawArray(state.unknown, unknown, g);							// Draw the various objects
		drawArray(state.breezes, breeze, g);
		drawArray(state.pits, pit, g);
		drawArray(state.stenches, stench, g);
		drawArray(state.wumpus, wumpus, g);
		drawArray(state.chests, chests[0], g);
		drawArray(state.filled_chests, chests[1], g);
		drawArray(state.goldbars, gold, g);
		drawArray(state.safe, safe, g);
		for(int i = 0; i < 4; i++)										// For the agents
			if(state.agents[i]!=null){ 									// If they are present
				BufferedImage icon = small_agent[i][state.moods[i]];	// Select image based on their mood
				g.drawImage(icon, state.shown_agents[i].width, state.shown_agents[i].height, icon.getWidth(), icon.getHeight(), null);
			}
		if(subjective){													// Believed worlds draw status images and notes
			BufferedImage status = agent_status_options[state.agentnr][state.moods[state.agentnr]];
			g.drawImage(status,cell_size,(height+3)*cell_size,status.getWidth(),status.getHeight(),null);
			if(state.moods[state.agentnr]==2) 							// Draw big chest if the agent is happy
				g.drawImage(bigchest,cell_size*3,(height+3)*cell_size,status.getWidth(),status.getHeight(),null);
			else if(state.moods[state.agentnr]==1)						// Draw grabbed gold if agent is content
				g.drawImage(grabbed_gold,cell_size,(height+3)*cell_size,status.getWidth(),status.getHeight(),null);
			g.setFont(small_font); 										// Set font for notices
			synchronized(state.notices){
				int y = cell_size+50;									// Start line
				for(String s : state.notices){							// Draw each notice
					g.drawString(s, cell_size*(2*width+2)+50,y);
					y += g.getFontMetrics().getHeight();				// Update line position
				}
			} 
		}
	}
	
	/**
	 * Draw an array of objects. Given an array of dimensions, a canvas and an image this method will paint for 
	 * each (x,y) from the dimensions the image on the canvas.
	 * @param positions Positions of the image.
	 * @param image Image to be drawn.
	 * @param g Canvas to paint on.
	 */
	public void drawArray(ArrayList<Dimension> positions, BufferedImage image, Graphics2D g){
		for(Dimension pos : positions){
			g.drawImage(image, pos.width, pos.height, image.getWidth(), image.getHeight(), null);
		}
	}
	
	/**
	 * Load an icon from the file system.
	 * @param icon_name File name to the icon.
	 * @return The BufferedImage that is created.
	 */
	public BufferedImage getIcon(String icon_name){
		try{ return ImageIO.read(new File(icon_name));
		} catch(Exception e){ System.out.println(icon_name);e.printStackTrace(); return null; }
	}
	
	/**
	 * Class to call the update method of the GUI.
	 * @author Bas Testerink, Utrecht University, The Netherlands
	 *
	 */
	private class FrameCaller implements Runnable {
		WumpusGUI panel; 		// Panel on which the world is drawn
		int fps;		 		// Frames per second
		boolean halt = false;   // Halt condition
		
		/**
		 * Constructor.
		 * @param panel The WumpusGUI to call.
		 * @param fps Frames per second. 30 means 30 times a second WumpusGUI.frame_update() will be called.
		 */
		public FrameCaller(WumpusGUI panel, int fps){
			this.panel = panel; 
			this.fps = fps;
		}
		
		public void run(){
			try{
				while(!halt){
					Thread.sleep(1000/fps); // Sleep a bit
					panel.frame_update();   // Update all data
					panel.repaint();		// Repaint the state
				}
			} catch(Exception e){ e.printStackTrace(); }
		}
		
		/**
		 * If this runnable is running and setHalt(true) is called, then this runnable will break the run loop.
		 * @param b New value of halt.
		 */
		public void setHalt(boolean b){
			halt = b;
		}
	}
}
