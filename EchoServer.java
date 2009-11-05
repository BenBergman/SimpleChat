// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

import java.io.*;
import ocsf.server.*;
import common.*;

import java.util.*;

/**
 * This class overrides some of the methods in the abstract 
 * superclass in order to give more functionality to the server.
 *
 * @author Dr Timothy C. Lethbridge
 * @author Dr Robert Lagani&egrave;re
 * @author Fran&ccedil;ois B&eacute;langer
 * @author Paul Holden
 * @version July 2000
 */
public class EchoServer extends AbstractServer 
{
  //Class variables *************************************************
  
  /**
   * The default port to listen on.
   */
  final public static int DEFAULT_PORT =5555;
  
  /**
   * The interface type variable. It allows the implementation of 
   * the display method in the client.
   */
  ChatIF serverUI;

  /**
   * HashMaps to store the registered users.
   * emails uses the email as the key with the uid as the value.
   * uids uses the uid as the key with the password as the value.
   */
  private HashMap emails = new HashMap(101);
  private HashMap uids = new HashMap(101);
  


  
  //Constructors ****************************************************
  
  /**
   * Constructs an instance of the echo server.
   *
   * @param port The port number to connect on.
   */
  public EchoServer(int port) 
  {
    super(port);
  }

   /**
   * Constructs an instance of the echo server.
   *
   * @param port The port number to connect on.
   * @param serverUI The interface type variable.
   */
  public EchoServer(int port, ChatIF serverUI) throws IOException
  {
    super(port);
    this.serverUI = serverUI;
  }

  
  //Instance methods ************************************************
  
  /**
   * This method handles any messages received from the client.
   *
   * @param msg The message received from the client.
   * @param client The connection from which the message originated.
   */
  public void handleMessageFromClient
    (Object msg, ConnectionToClient client)
  {
    if (msg.toString().startsWith("#login "))
    {
      if (client.getInfo("loginID") != null)
      {
        try
        {
          client.sendToClient("You are already logged in.");
        }
        catch (IOException e)
        {
        }
        return;
      }
      client.setInfo("loginID", msg.toString().substring(7));
    }
    else if (msg.toString().toLowerCase().startsWith("#reg ")) // might want to clean up the if nest. Use command pattern
    {
      // register uid
      String[] commands = ((String)msg).split(" ");
      if (commands.length != 4)
      {
        try
        {
          client.sendToClient("usage: #reg <uid> <pw> <email>");
        }
        catch(IOException e) {}
        return;
      }

      // check if uid exists
      // "uid has already been used by another user. Please choose another uid."

      if (uids.containsKey(commands[1]))
      {
        // notify user that uid is taken
        try
        {
          client.sendToClient(commands[1] + " has already been used by another user. Please choose another uid.");
        }
        catch(IOException e) {}
        return;
      }
      
      // check if email already used
      // "email already been used by another user. Please choose another email or request to send forgotten uid and/or pw."
      
      if (emails.containsKey(commands[3]))
      {
        // notify user that uid is taken
        try
        {
          client.sendToClient(commands[3] + " has already been used by another user. Please choose another email or request "
                                          + "to send forgotten uid and/or pw.");
        }
        catch(IOException e) {}
        return;
      }

      // register user
      // "Registration accepted. Please keep your uid and pw in a safe and secure location for future reference."

      emails.put(commands[3], commands[1]);
      uids.put(commands[1], commands[2]);
      // notify user that uid and pass is stored
      try
      {
        client.sendToClient("Registration accepted. Please keep your uid and pw in a safe "
                          + "and secure location for future reference.");
      }
      catch(IOException e) {}
    }
    else if (msg.toString().startsWith("#regInfo"))
    {
      // email user info
      String[] commands = ((String)msg).split(" ");
      if (commands.length != 4)
      {
        try
        {
          client.sendToClient("usage: #regInfo <email>");
        }
        catch(IOException e) {}
        return;
      }

      // check to see if email is registered
      if (emails.containsKey(commands[3]))
      {
        try
        {
          client.sendToClient("Sorry, " + commands[3] + " does not exist in our records. Please use the " +
            "email address with which the account has been registered, or register another account.");
        }
        catch(IOException e){}
        return;
      }

      try
      {
        client.sendToClient("An email will be sent to the email address " + commands[3] + " giving the " + 
                            "uid and pw associated with that account.");
      }
      catch (IOException e) {}
    }
    else
    {
      if (client.getInfo("loginID") == null)
      {
        try
        {
          client.sendToClient("You need to login before you can chat.");
          client.close();
        }
        catch (IOException e) {}
        return;
      }
      System.out.println("Message received: " + msg + " from \"" + 
        client.getInfo("loginID") + "\" " + client);
      this.sendToAllClients(client.getInfo("loginID") + "> " + msg);
    }
  }

  /**
   * This method handles all data coming from the UI
   *
   * @param message The message from the UI
   */
  public void handleMessageFromServerUI(String message)
  {
    if (message.charAt(0) == '#')
    {
      runCommand(message);
    }
    else
    {
      // send message to clients
      serverUI.display(message);
      this.sendToAllClients("SERVER MSG> " + message);
    }
  }

  /**
   * This method executes server commands.
   *
   * @param message String from the server console.
   */
  private void runCommand(String message)
  {
    // run commands
    // a series of if statements

    if (message.equalsIgnoreCase("#quit"))
    {
      quit();
    }
    else if (message.equalsIgnoreCase("#stop"))
    {
      stopListening();
    }
    else if (message.equalsIgnoreCase("#close"))
    {
      try
      {
        close();
      }
      catch(IOException e) {}
    }
    else if (message.toLowerCase().startsWith("#setport"))
    {
      if (getNumberOfClients() == 0 && !isListening())
      {
        // If there are no connected clients and we are not 
        // listening for new ones, assume server closed.
        // A more exact way to determine this was not obvious and
        // time was limited.
        int newPort = Integer.parseInt(message.substring(9));
        setPort(newPort);
        //error checking should be added
        serverUI.display
          ("Server port changed to " + getPort());
      }
      else
      {
        serverUI.display
          ("The server is not closed. Port cannot be changed.");
      }
    }
    else if (message.equalsIgnoreCase("#start"))
    {
      if (!isListening())
      {
        try
        {
          listen();
        }
        catch(Exception ex)
        {
          serverUI.display("Error - Could not listen for clients!");
        }
      }
      else
      {
        serverUI.display
          ("The server is already listening for clients.");
      }
    }
    else if (message.equalsIgnoreCase("#getport"))
    {
      serverUI.display("Currently port: " + Integer.toString(getPort()));
    }
  }
    
  /**
   * This method overrides the one in the superclass.  Called
   * when the server starts listening for connections.
   */
  protected void serverStarted()
  {
    System.out.println
      ("Server listening for connections on port " + getPort());
  }
  
  /**
   * This method overrides the one in the superclass.  Called
   * when the server stops listening for connections.
   */
  protected void serverStopped()
  {
    System.out.println
      ("Server has stopped listening for connections.");
  }

  /**
   * Run when new clients are connected. Implemented by Benjamin Bergman,
   * Oct 22, 2009.
   *
   * @param client the connection connected to the client
   */
  protected void clientConnected(ConnectionToClient client) 
  {
    // display on server and clients that the client has connected.
    String msg = "A Client has connected";
    System.out.println(msg);
    this.sendToAllClients(msg);
  }

  /**
   * Run when clients disconnect. Implemented by Benjamin Bergman,
   * Oct 22, 2009
   *
   * @param client the connection with the client
   */
  synchronized protected void clientDisconnected(
    ConnectionToClient client)
  {
    // display on server and clients when a user disconnects
    String msg = client.getInfo("loginID").toString() + " has disconnected";

    System.out.println(msg);
    this.sendToAllClients(msg);
  }

  /**
   * Run when a client suddenly disconnects. Implemented by Benjamin
   * Bergman, Oct 22, 2009
   *
   * @param client the client that raised the exception
   * @param Throwable the exception thrown
   */
  synchronized protected void clientException(
    ConnectionToClient client, Throwable exception) 
  {
    String msg = client.getInfo("loginID").toString() + " has disconnected";

    System.out.println(msg);
    this.sendToAllClients(msg);
  }

  /**
   * This method terminates the server.
   */
  public void quit()
  {
    try
    {
      close();
    }
    catch(IOException e)
    {
    }
    System.exit(0);
  }

  /**
   * This method sends an email.
   * Method borrowed from:
   *    http://www.javacommerce.com/displaypage.jsp?name=javamail.sql&id=18274
   */
  /*
  public void postMail(String[] recipients, String subject, String message,
    String from) throws MessagingException
  {
    boolean debug = false;

    //Set the host smtp address
    Properties props = new Properties();
    props.put("mail.smtp.host", "smtp.ee.umanitoba.ca");

    //create some properties and get the default session
    Session session = Session.getDefaultInstance(props, null);
    session.setDebug(debug);

    //create a message
    Message msg = new MimeMessage(session);

    // set the from and to addresses
    InternetAddress addressFrom = new InternetAddress(from);
    msg.setFrom(addressFrom);

    InternetAddress[] addressTo = new InternetAddress[recipients.length];
    for (int i = 0; i < recipients.length; i++)
    {
      addressTo[i] = new InternetAddress(recipients[i]);
    }
    msg.setRecipients(Message.RecipientType.TO, addressTo);

    // setting the subject and content type
    mst.setSubject(subject);
    msg.setContent(message, "text/plain");
    Transport.send(msg);
  }
*/

  //Class methods ***************************************************
  
  /**
   * This method is responsible for the creation of 
   * the server instance (there is no UI in this phase).
   *
   * @param args[0] The port number to listen on.  Defaults to 5555 
   *          if no argument is entered.
   */
  public static void main(String[] args) 
  {
    int port = 0; //Port to listen on

    try
    {
      port = Integer.parseInt(args[0]); //Get port from command line
    }
    catch(Throwable t)
    {
      port = DEFAULT_PORT; //Set port to 5555
    }
	
    EchoServer sv = new EchoServer(port);
    
    try 
    {
      sv.listen(); //Start listening for connections
    } 
    catch (Exception ex) 
    {
      System.out.println("ERROR - Could not listen for clients!");
    }
  }
}
//End of EchoServer class
