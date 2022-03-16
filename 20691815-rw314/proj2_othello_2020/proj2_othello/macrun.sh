#*******************************************************************************************************
#* ./run.sh 
#*	- This option uses the settings in the game.json config file. 
#*
#* ./run.sh pathToPlayer1 pathToPlayer2 int_val_time_out_in_seconds num_processes
#*	- If there is no game.json yet, or you want to set new parameters.
#*
#*	~/path/to/FirstplayerExecutable  
#*		==> this needs to be in a folder, path cannot be just the player name.
#*		==> tested with full path from home 
#*	~/path/to/SecondplayerExecutable  
#*		==> this needs to be in a folder, path cannot be just the player name.
#*		==> tested with full path from home 
#*	time 
#*		==> this is the time a player has to make a move, unit is seconds, doubles are allowed.
#*	num_processes	
#*		==> this is the number of threads to run your player with; the default is 1
#*
#* 	
#******************************************************************************************************
if [ -z "$1" ] || [ -z "$2" ]|| [ -z "$3" ]|| [ -z "$4" ]; then

echo "Usage: ./run.sh player1 player2 int_val_time_out_in_seconds num_processes"
#exit
else
echo "{
	\"numPlayers\": 2,
	\"threads\": $4,
	\"time\": $3,
	\"path1\": \"$1\",
	\"path2\": \"$2\"
}" > game.json
fi
#Stops gameserver if it is already running
#fuser -n tcp -k 61234 
pids="$(lsof -t -i:61234)"
if [ ! -z "$pids" ]; then 
	kill "$pids"
fi

currentpwd=$PWD
#Starts the server in a new terminal
#gnome-terminal -x sh -c "java -jar IngeniousFramework.jar server; bash"
#for macOS
osascript -e 'tell app "Terminal" 
	do script "java -jar '$currentpwd/IngeniousFramework.jar' server; bash" 
end tell'
sleep 2
#Starts the othello lobby
java -jar IngeniousFramework.jar create -config "game.json" -game "OthelloReferee" -lobby "mylobby"

#runs a player named foo
#gnome-terminal -x sh -c "java -jar IngeniousFramework.jar client -username foo -engine za.ac.sun.cs.ingenious.games.othello.engines.OthelloMPIEngine -game OthelloReferee -hostname localhost -port 61234; bash"
#for macOS
osascript -e 'tell app "Terminal" 
	do script "java -jar '$currentpwd/IngeniousFramework.jar' client -username foo -engine za.ac.sun.cs.ingenious.games.othello.engines.OthelloMPIEngine -game OthelloReferee -hostname localhost -port 61234"
end tell'

#This will pipe to file instead
#gnome-terminal -x sh -c "java -jar IngeniousFramework.jar client -username foo -engine za.ac.sun.cs.ingenious.games.othello.engines.OthelloMPIEngine -game OthelloReferee -hostname localhost -port 61234 > foo"

#runs a player named bar 
#gnome-terminal -x sh -c "java -jar IngeniousFramework.jar client -username bar -engine za.ac.sun.cs.ingenious.games.othello.engines.OthelloMPIEngine -game OthelloReferee -hostname localhost -port 61234; bash"
#for macOS
osascript -e 'tell app "Terminal" 
	do script "java -jar '$currentpwd/IngeniousFramework.jar' client -username bar -engine za.ac.sun.cs.ingenious.games.othello.engines.OthelloMPIEngine -game OthelloReferee -hostname localhost -port 61234"
end tell'

#This will pipe to file instead
#gnome-terminal -x sh -c "java -jar IngeniousFramework.jar client -username bar -engine za.ac.sun.cs.ingenious.games.othello.engines.OthelloMPIEngine -game OthelloReferee -hostname localhost -port 61234 > bar"


