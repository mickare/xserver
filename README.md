XServer
=======
[![Build Status](https://travis-ci.org/mickare/xserver.svg?branch=master)](https://travis-ci.org/mickare/xserver)
[![MIT licensed](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE)


Description
-----------
XServer is a library to send messages between minecraft servers. It is designed to be used by other plugins.

With this plugin it's simple to send byte arrays to other servers and receive them.
It's usefull if you use plugins on multiple servers that you want to communicate via sockets.


You, the developer, no more have to think about creating sockets and keeping the connection open.
XServer handles all complicated stuff and offers a nice solution to send messages.

It automatically queues outgoing messages and handles incoming messages with an own EventHandler.

Your servers are protected via passwords and are managed via MySql.

There are currently 2 Versions of XServer: one for Bukkit and one for BungeeCord.
XServer has a Core-Module and a API, so these are released in the future, so not only on Bukkit and on BungeeCord can be run XServer.

Installation & Usage
--------------------

Visit http://dev.bukkit.org/bukkit-plugins/xserver-com/
