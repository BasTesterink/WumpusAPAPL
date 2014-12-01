:- dynamic percept/3.

shortest_path_from(X,Y,Goal,RevPath):-
	pp_expand_node([],[node(X,Y)],-1,[],Goal,Path),reverse_path(Path,[],RevPath).
search_goal(X,Y,location(X,Y)).
search_goal(X,Y,safe):- safe(X,Y). 	
search_goal(X,Y,not_deadly):- not(is_deadly(X,Y)).  
pp_illegal(X,Y):- not(pp_in_grid(X,Y)).
pp_illegal(X,Y):- is_deadly(X,Y).
pp_grid_width(5).
pp_grid_height(5).

reverse_path(false,_,false).						
reverse_path([A],ACC,[A|ACC]). 							
reverse_path([A,B|C],ACC,Path):-					 
	reverse_path([B|C],[A|ACC],Path).

path(Path):-
	pp_expand_node([],[node(1,5)],-1,[],location(1,3),Path),reverse_path(Path,[],RevPath),print(RevPath).
pp_expand_node([],[],_,_,_,false).
pp_expand_node([],NewQueue,S,Nodes,Goal,Path):-
	NewQueue \= [],
	NewS is S+1,
	pp_expand_node(NewQueue,[],NewS,Nodes,Goal,Path).
pp_expand_node([node(X,Y)|_],_,S,Nodes,Goal,[node(X,Y)|Path]):-
	search_goal(X,Y,Goal),
	pp_obtain_path(node(X,Y),Nodes,S,Path).
pp_expand_node([node(X,Y)|Rest],NQ,S,Nodes,Goal,Path):-
	not(search_goal(X,Y,Goal)),
	pp_add_node(X+1,Y,NQ,NQ2,Nodes),
	pp_add_node(X-1,Y,NQ2,NQ3,Nodes),
	pp_add_node(X,Y+1,NQ3,NQ4,Nodes),
	pp_add_node(X,Y-1,NQ4,NQ5,Nodes),
	pp_expand_node(Rest,NQ5,S,[node(X,Y,S)|Nodes],Goal,Path).

pp_add_node(Ux,Uy,L,L,Nodes):-
	X is Ux, Y is Uy,
	(member(node(X,Y,_),Nodes);
	member(node(X,Y),L);
	pp_illegal(X,Y)).
pp_add_node(Ux,Uy,L,[node(X,Y)|L],Nodes):-
	X is Ux, Y is Uy,
	not(member(node(X,Y,_),Nodes);
	member(node(X,Y),L);
	pp_illegal(X,Y)).
	
pp_obtain_path(_,_,1,[]).
pp_obtain_path(node(X,Y),Nodes,S,[node(X2,Y2)|Rest]):-
	NewS is S - 1,
	member(node(X2,Y2,NewS),Nodes),
	1 is (X2-X)*(X2-X) + (Y2-Y)*(Y2-Y),
	pp_obtain_path(node(X2,Y2),Nodes,NewS,Rest).
	
pp_in_grid(X,Y):-
	pp_grid_width(W),						
	pp_grid_height(H),
	X < W, X > 0,							
	Y < H, Y > 0.
	
	%%%%%%%%%%%%%%%%%%%%%
	%%%%%%%%%%%%%%%%%%%%%
	% A spot is safe if there cannot be a pit or wumpus in that tile. Works only for unexplored cells.
safe(X,Y):- not(outside(X,Y)),impossible(pit,X,Y),impossible(wumpus,X,Y). 
% A spot is deadly there is either a wumpus or a pit present.
is_deadly(X,Y):- wumpus(X,Y).
is_deadly(X,Y):- pit(X,Y).

% (for unexplored tile) There cannot be a pit on (x,y) if (x,y) is not a candidate pit spot for any breeze.
impossible(pit,X,Y):- not(visited(X,Y)),not(is_candidate(breeze,X,Y)). 
% (for unexplored tile) There cannot be a Wumpus on (x,y) if (x,y) is not a candidate Wumpus spot for any stench.
impossible(wumpus,X,Y):- not(visited(X,Y)),not(is_candidate(stench,X,Y)). 

% You know where a pit is if one of its breezes has only 1 candidate, same for wumpus and stench.
wumpus(X,Y):- only_candidate(stench,X,Y).
pit(X,Y):- only_candidate(breeze,X,Y).

% If an (x,y) position is the only candidate pit/wumpus for a percept, then there must be at least one adjacent
% percept entry for which holds that all adjacent tiles to that entry, except for (x,y) cannot hold a pit/wumpus.
only_candidate(Percept,X,Y):- X2 is X+1, percept(Percept,X2,Y), candidates(Percept,X2,Y,[node(X,Y)]).
only_candidate(Percept,X,Y):- X2 is X-1, percept(Percept,X2,Y), candidates(Percept,X2,Y,[node(X,Y)]).
only_candidate(Percept,X,Y):- Y2 is Y+1, percept(Percept,X,Y2), candidates(Percept,X,Y2,[node(X,Y)]).
only_candidate(Percept,X,Y):- Y2 is Y-1, percept(Percept,X,Y2), candidates(Percept,X,Y2,[node(X,Y)]).

% Gather the candidates for a percept on cell (x,y).
candidates(Percept,X,Y,R):-
	inspect(Percept,0,-1,X,Y,[],C1),
	inspect(Percept,-1,0,X,Y,C1,C2),
	inspect(Percept,0,1,X,Y,C2,C3),
	inspect(Percept,1,0,X,Y,C3,R).

% Check whether tile (x,y) is a candidate pit/wumpus, if so add it. 
inspect(Percept,Dx,Dy,X,Y,Prev,Next):- 
	NewX is X+Dx, NewY is Y+Dy,   
	is_candidate(Percept,NewX,NewY),!, % We have to cut before the Next var is bound, 
	Next = [node(NewX,NewY)|Prev].     % otherwise a failure of its unification will jump to the second entry for inspect/7.
inspect(_,_,_,_,_,Next,Next).	   % If not a candidate just continue

% Candidate check, clearly pits/wumpi (? plural wumpus?) must be inside the world and cannot be visited.
% Plus adjacent to the candidate must not be a cleared spot.
is_candidate(Percept,X,Y):-
	not(outside(X,Y)),
	not(visited(X,Y)),
	X2 is X+1,X3 is X-1, Y2 is Y+1, Y3 is Y-1,
	not(is_clear(Percept,X2,Y)), % Next to the candidate pit must not be a spot of which we know it is clear
	not(is_clear(Percept,X3,Y)),
	not(is_clear(Percept,X,Y2)),
	not(is_clear(Percept,X,Y3)).
	
% A spot is clear if it is both visited and the agent did not perceive the searched percept. 
% Note that for unvisited spots we assume that they have a breeze/stench on them.
is_clear(Percept,X,Y):- 
	visited(X,Y),
	not(percept(Percept,X,Y)). 
	position(1,1). 				% The given start position
	visited(1,1). 				% The start position is already visited
	chest(1,1).					% Location of the chest
	gold(X,Y):- glitter(X,Y). 	% Glitter implies gold 
	wallY(0). 					% The intially known walls
	wallY(5).
	wallX(0). 
	wallX(5).
	
	% The emotional state of the agent (looks nice in GUI)
	mood(happy):- chest(X,Y),gold(X,Y).          % Goal reached so the agent is happy
	mood(content):- has(gold).					 % Agent has grabbed the gold so is content
	mood(angry):- not(has(gold)),not(gold(1,1)). % Mood is angry when no gold is found or delivered
	
	% Given an X and Y, the third arguments becomes the direction the agent should move to.
	direction(GoalX,_,left):- position(X,_),X>GoalX.  
	direction(GoalX,_,right):- position(X,_),X<GoalX. 
	direction(_,GoalY,down):- position(_,Y),Y>GoalY. 
	direction(_,GoalY,up):- position(_,Y),Y<GoalY.
	
	% Determine the new position given a direction. Fails if the move is - to the best knowledge of the agent - impossible.
	new_position(up,X,Y):- upwards(X,Y).							
	new_position(down,X,Y):- downwards(X,Y).
	new_position(left,X,Y):- leftwards(X,Y).
	new_position(right,X,Y):- rightwards(X,Y).
	
	% Determine the new position after an upwards/downwards/leftwards/rightwards movement.
	upwards(X,Y):- position(X,Y2), Y is Y2+1, not(outside(X,Y)).
	downwards(X,Y):- position(X,Y2), Y is Y2-1, not(outside(X,Y)).
	leftwards(X,Y):- position(X2,Y), X is X2-1, not(outside(X,Y)).
	rightwards(X,Y):- position(X2,Y), X is X2+1, not(outside(X,Y)).
	
	% Check whether an (x,y)-coordinate is outside of the world.
	outside(X,_):- wallX(X).
	outside(_,Y):- wallY(Y). 