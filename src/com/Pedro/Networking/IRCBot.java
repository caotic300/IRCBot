package com.Pedro.Networking;

import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This is a Spam IRCBot Client that communicates with the server or other clients, this IRCBot allows to respond and send messages between
 * client and server, server and server and also ask other stuff such as the time and the date that is it, and also to print the
 * message of the day...
 * The purpose of this IRCBot is to respond to received messages and allows to get the name of the user on the channel
 * and you can also send a message through different channels.
 */
public class IRCBot implements Runnable {

    private final int port;
    private String server;
    private String addr;
    private String nick;
    private String user;
    private static final String LOVE = "LOVE ";
    private static final String QUIT = "QUIT ";
    private static final String PART = "PART ";
    private static final String PRIVMSG = "PRIVMSG ";
    private static final String NICK = "NICK ";
    private static final String JOIN = "JOIN ";
    private static final String USER = "USER ";
    private static final String PING = "PING ";
    private static final String TIME = "TIME ";
    private static final String LIST = "LIST ";
    private static final String HELP = "HELP";
    private static final String SENDTOCHANNELS = "SENDTOCHANNELS ";
    private static String messageOfDay = "MESSAGEOFTHEDAY";

    private CopyOnWriteArrayList<String> channels;

    public IRCBot() {
        this.addr = "127.0.0.1";
        this.port = 6667;
        this.user = "caotic300";
        this.server = "selsey.nsqdc.city.ac.uk";
        this.nick = "pedroBot2";
        this.channels = new CopyOnWriteArrayList<>();
    }


    @Override
    public void run() {
        try (
                Socket socket = new Socket(addr, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()))) {
            System.out.println("Client connected");

            String response;
            String msg;

            out.println(registerNick());
            out.println(registerUser());
            out.println(join("help"));

            response = in.readLine();

            System.out.println(response);

            out.println(LIST);

            boolean msgs = true;
            boolean isEndOfList = response.contains("End") && response.contains("of") && response.endsWith("LIST");
            boolean isStartOfList = false;

            while ((response = in.readLine()) != null) {

                System.out.println(response);

                //if the list command is read on the response set true isStartOfList
                if (response.contains("Channel") && response.contains(":Users")) {
                    isStartOfList = true;

                }
                //while is in the list find all channels else set one to false so does not repeat
                if (isStartOfList && !isEndOfList) {
                    findAllChannels(response);
                } else if (isEndOfList) {
                    isEndOfList = false;

                }

                if (msgs) {
                    //uncomment to test if it sends to all channels
                    //out.println(generatePrivateMessage("PedroBot2", "SENDTOCHANNELS " + "HELLO CHANNELS"));
                    getOptions(response, out);
                    msgs = false;
                }

                if ((response.endsWith(HELP))) {
                    getOptions(response, out);
                }

                if (response.startsWith(PING)) {
                    // We must respond to PINGs to avoid being disconnected.
                    System.out.println(response);
                    out.println("PONG " + response.substring(5) + "\r\n");
                    out.println(generatePrivateMessage(channels.get(0), "I got pinged!"));
                }

                if (response.contains(PRIVMSG) && response.contains(nick)) {
                    msg = "The thing I like the most is...!";
                    out.println(sendMessage(response, msg));
                }

                if (response.startsWith(JOIN)) {
                    out.println("PRIVMSG " + channels.get(0) + " :Hello!");
                }

                if (response.startsWith(QUIT)) {
                    break;
                }

                if ((response.contains("date")) && response.contains("is")) {

                    out.println(generatePrivateMessage(findNick(response), "Thank you for asking the time, your date is: " + LocalDate.now().toString()));
                }

                if ((response.contains("time")) && response.contains("is")) {

                    out.println(generatePrivateMessage(findNick(response), "Thank you for asking the time, your time is: " + LocalTime.now().toString()));
                }

                if (response.startsWith(LOVE)) {

                    out.println(sendMessage(response, "I love you too"));
                }

                if (response.startsWith(messageOfDay) || response.startsWith(messageOfDay.toLowerCase())) {
                    messageOfTheDay(response, out);
                }

                //might get a Connection refused, too many connections from your IP address
                if (response.contains(SENDTOCHANNELS)) {

                    sendToAllChannels(response, out);
                }

                if (findMessageWithinResponse(response).startsWith("SERVER TIME")) {
                    out.println(sendMessage(response, getTime()));
                }

                // out.println("TIME " + server);
                //out.println(generatePrivateMessage(findNick(response), server));
            }

        } catch (IOException e) {

        }
    }


    /**
     * Formats the nick registration string to register through the irc protocol
     *
     * @return String - The nick registration following the irc protocol
     */
    public String registerNick() {

        return NICK + nick + "\r\n";
    }

    /**
     * Formats the user registration string to register through the irc protocol
     *
     * @return String - The user registration following the irc protocol
     */
    public String registerUser() {

        return USER + user + " 0 * :" + "Pedro Londono";
    }

    /**
     * Formats the joining to  a channel through the irc protocol
     *
     * @param channel
     * @return String - The joining registration following the irc protocol
     */
    public String join(String channel) {
        return JOIN + initiateChannels(channel);
    }

    public String part(String channel) {
        return JOIN + initiateChannels(channel);
    }

    /**
     * Formats the channel of the string
     *
     * @param channel - The name of the channel you want use
     * @return String - The formatted channel using irc protocol
     */
    public String initiateChannels(String channel) {
        String str = "#" + channel;
        if (!channels.contains(str)) {
            channels.add(str);
        }

        return str;
    }


    public String generatePrivateMessage(String dest, String msg) {
        return PRIVMSG + dest + " :" + msg + "\r\n";
    }

    /**
     * Shows the options that this client provides
     *
     * @param response - the message received from either a client or either a server
     * @param out      - sends the help message to the client that sent the response through the network
     */
    public void getOptions(String response, PrintWriter out) {

        out.println(sendMessage(response, "This is PedroBot2 and I am here to help and show you my options"));
        out.println(sendMessage(response, "Type \"PING \"  -- to receive an awesome response "));
        out.println(sendMessage(response, "Type \"MESSAGEOFTHEDAY\"  -- to receive the message of the day "));
        out.println(sendMessage(response, "Type \"LOVE\" -- to receive a love message"));
        out.println(sendMessage(response, "Type \"QUIT \" -- to quit"));
        out.println(sendMessage(response, "Type \"SENDTOCHANNELS <yourMessage>\" -- to send a message to all the channels"));

    }


    /**
     * Gets the message to send either to a client or server,
     * by checking who is comunicating with this bot, in case it is the server the
     * message is sent through the first channel, else it is sent privately to the client.
     *
     * @param response - The response from the server
     * @param msg      - The message that the bot replies
     * @return String - The message to send to the network
     */
    public String sendMessage(String response, String msg) {

        String receiver;
        String nick = findNick(response);
        boolean throughChannel = server.equals(nick);

        if (!throughChannel) {
            receiver = generatePrivateMessage(nick, msg);

        } else {
            receiver = generatePrivateMessage(channels.get(0), msg);
        }

        return receiver;
    }

    /**
     * Gets the message to send through the channel.
     *
     * @param channel  - The channel to send through
     * @param msg      - The message that the bot replies
     * @return String - The message to send to the network
     */
    public String sendMessageToChannels(String channel, String msg) {

        String receiver;
        receiver = generatePrivateMessage(channels.get(channels.indexOf(channel)), msg);

        return receiver;
    }

    /**
     * Finds the nick of the response sent by the server to this client, this can either be the server
     * itself or a client that wants to comunicate to this bot.
     *
     * @param response - The response from the server sent to this client
     * @return String - The nick of the sender
     */
    public String findNick(String response) {
        int index = 0;
        String nickStr = "";
        String exclamation = "!";
        String space = " ";
        int indexEx = response.indexOf(exclamation);

        int indexSpace = response.indexOf(space);

        if (response.startsWith(":")) {
            index = 1;
        }

        if (response.contains(exclamation)) {
            nickStr = response.substring(index, indexEx);

        } else if (response.contains(space)) {
            nickStr = response.substring(index, indexSpace);

        }

        return nickStr;
    }


    public String findAllNicks(String response) {
        int index = response.lastIndexOf(":");

        return response.substring(index);
    }

    /**
     * Finds the Message part of the server message received by this client
     * @param response - The response of the server.
     * @return String - The Message of the user
     */
    public String findMessageWithinResponse(String response) {
        int index = response.lastIndexOf(":");
        String msg = response.substring(index + 1);
        return msg;
    }
    /**
     * Finds the channels from the server response
     *
     * @param response - The response from either the server
     * @return String - The channel found from the response
     */
    public String findChannelFromResponse(String response) {
        int indexTo = response.lastIndexOf("#") + 1;
        String tmp = response.substring(indexTo);
        String[] strings = tmp.split(" ");
        String string = strings[0];
        initiateChannels(string);

        return string;
    }


    /**
     * Prints the message of the day for the use
     *
     * @param response - the response from either a server or a client using this bot
     * @param out       - The object that sends the request to the server
     */
    public void messageOfTheDay(String response, PrintWriter out) {
        out.println(sendMessage(response, "You asked for the Message of the day, here you go:"));
        out.println(sendMessage(response, "I wish you a really good day sir. I hope all your dreams come true"));
        out.println(sendMessage(response, "All of the people is looking to make their dreams true, and I wish you make your's true"));
        out.println(sendMessage(response, "People we have to make our dreams true!!!"));

    }

    /**
     * Sends a message to all the channels available in the server. The message is sent every 5 seconds
     * @param response - The response from the server,
     * @param out - the PrintWriter object to send the message
     */
    public void sendToAllChannels(String response, PrintWriter out) {
        String message = findMessageWithinResponse(response).substring(SENDTOCHANNELS.length());
        String firstChannel = channels.get(0);
        part(firstChannel);
        try {

            for (String channel : channels) {

                join(channel);
                out.println(sendMessageToChannels(channel, message));
                part(channel);

                Thread.sleep(5000);

                System.out.println("pass by");
            }
            join(firstChannel);
        } catch (InterruptedException e) {

        }
    }

    /**
     * Finds all the channels that are currently created in the server, the LIST command must be called before
     * calling this method
     *
     * @param response - The response from the server after calling the LIST command
     */
    public void findAllChannels(String response) {

        if (response.contains("#")) {
            String channel = findChannelFromResponse(response);
            System.out.println("channel: " + channel);
        }
    }


    /**
     * Returns the time provided by the irc server
     *
     * @return String - The time
     */
    public String getTime() {

        return TIME + server;
    }

    public static void main(String[] args) {
        new IRCBot().run();
    }

}

