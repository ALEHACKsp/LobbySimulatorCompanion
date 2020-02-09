# What is LOOP?

LOOP (which comes from "LObby simulatOr comPanion") is a tool for helping DBD ("Dead By Daylight") survivor players
have a better experience in the game, especially while waiting in the match lobbies.

LOOP can show you information about the server you are connecting to (such as the location), 
as well as information about the killer (such as how many times you played against them or how much time you spent 
playing against that person). It will let you rate the killer and even give you a space
where to add your own personal notes.
In addition to that, LOOP provides you with a bunch of statistics. Wonder how much time you spend on lobby queues vs. 
actually playing? or how many times you survived today (or this week, or during year, etc.)? 


# What does it look like?
Here's a sample screenshot:

![](docs/images/lobby_sample.png)

You can see the LOOP bar showing the connection status, server information, killer information (in addition to your
own personal notes for rating them) and a bunch of statistics that include survival info match times, and queue times. 


# How to install?
1. Make sure you satisfy the system requirements:
    * Microsoft Windows (preferrably, Windows 10, which is what we test in)
    * Java Runtime of at least version 8 (https://java.com/en/download/)
1. Download loop.exe from the [releases page](https://github.com/nickyramone/LobbySimulatorCompanion/releases).
1. Create a folder where you would like to install this app (Example, under C:\Program Files\LobbySimulatorCompanion) 
   and place the exe there.
   * Tip: Don't throw it under the desktop. Instead, create a folder where to contain this application, and then create
          a desktop launcher if you want.


# How to run?
1. Double click on loop.exe on your installation folder.\
  **NOTE:** You may need to right-click on the file, select Properties, and choose "Unblock" if it appears below "Attributes".
1. If the application started successfully, you should see at least these two files in the installation directory:\
   loop.settings.ini, loop.hosts.dat
1. **Advanced usage:**\
   If you want to avoid having to select the network device IP every time you start the app, edit loop.settings.ini and make sure you have this line: network.interface.autoload = true


# Is my data (like stats or personal notes) stored in any servers?
No. We don't publish any data to any servers, which means that if you lose your installation folder 
(or format your hard disk), your data will be lost.
To prevent this, we recommend doing backups of the 'loop.dat' file.


## Can I get banned by EAC (Easy Anti Cheat) for using LOOP?
Short answer: No.


While with these things you can never have a 100% guarantee, here's why you shouldn't get banned:
1) This app does not modify or interfere with DBD in any way.
2) It is not aimed at cheating but at getting more stats that the game does not provide.
3) It is a completely external/independent desktop application, which does not hook to any Steam/DBD process.
4) It may read network traffic for tracking connections but never sends packets or fiddles with them.
5) It doesn't alter the game mechanics in any way.
6) It is based on the MLGA codebase, which was not banned by EAC before.
7) The tool does nothing that you couldn't do manually. It just organizes and simplifies the information.
8) I have contacted EAC (Easy Anti Cheat) and they replied that it's not in their interest to ban these kinds of applications,
   unless explicitly asked by the game developers. Their "main focus is on the specifically crafted cheating tools".


## Why did you create this fork of MLGA (MakeLobbiesGreatAgain)?
LOOP has started being been built on top of the MLGA code, but I wanted to make significant changes to the codebase as well as the purpose of the project.
MLGA had become more of a generic P2P tool for multiple games, while LOOP is very oriented towards DBD survivor players.
The code has changed so much with so many different features and core connection mechanics that it has become a project of its own.


## Hey! My antivirus is giving me a warning about the exe file!
Some antivirus throw a false positive. Loop is coded in Java, which is normally provided in jar format.
In order to distribute a more user-friendly format, the jar is wrapped in an exe bootsrapper.
This technique sometimes triggers some anti-virus heuristics.
If you want, you can always just use the jar file.

