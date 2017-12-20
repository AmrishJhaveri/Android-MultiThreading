package com.example.amrish.project4;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

public class MainActivity extends Activity {

    private final String TAG = "MainActivity";
    private Button btnStart;
    private Button btnStartWin;
    private TextView textView;
    private TextView textView2;
    private TextView textView3;

    /* UI handler*/
    Handler mUiHandler = new Handler(Looper.getMainLooper()) {
        /**
         * Subclasses must implement this to receive messages.
         *
         * @param msg
         */
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                //checking the status of the player. Based on the count of each player, decision is made on who should make the next move.
                case CHECK_STATUS_PLAYER:
                    Bundle bundle = msg.getData();
                    String playerName = (String) bundle.getString("NAME");
                    int[] guessResponse=(int[])bundle.getIntArray("GUESSES");

                    Log.i(TAG, "Before countPlayer1:" + countPlayer1 + "\tcountPlayer2:" + countPlayer2 + "\t name:" + playerName);

                    // When Player 2 has won the game
                    if(THREAD_NAME_PLAYER_1.equalsIgnoreCase(playerName) && guessResponse[0]==4){
                        mThread1.executeLooperQuit();
                        mThread2.executeLooperQuit();
                        postGameStatus("Player 2 WON!!!");
                    }
                    // When Player 1 has won the game
                    else if(THREAD_NAME_PLAYER_2.equalsIgnoreCase(playerName) && guessResponse[0]==4){
                        mThread1.executeLooperQuit();
                        mThread2.executeLooperQuit();
                        postGameStatus("Player 1 WON!!!");
                    }
                    //Player 1 passed the check status message and is eligible to make a guess
                    else if (THREAD_NAME_PLAYER_1.equalsIgnoreCase(playerName) && countPlayer1 < NO_OF_GUESSES) {
                        //checking if the count of player1 is less than or equal to player2. Also if the handler is created or not.
                        if (countPlayer1 <= countPlayer2 && mThread1.getFirstPlayerHandler()!=null) {
                            //send message to make a guess
                            mThread1.getFirstPlayerHandler().sendMessage(mThread1.getFirstPlayerHandler().obtainMessage(MAKE_GUESS_AND_SEND));
                            countPlayer1++;
                        }
                    }
                    //Player 2 passed the check status message and is eligible to make a guess
                    else if (THREAD_NAME_PLAYER_2.equalsIgnoreCase(playerName) && countPlayer2 < NO_OF_GUESSES) {
                        //checking if the count of player2 is less than or equal to player1. Also if the handler is created or not.
                        if (countPlayer2 <= countPlayer1 && mThread2.getSecondPlayerHandler()!=null) {
                            mThread2.getSecondPlayerHandler().sendMessage(mThread2.getSecondPlayerHandler().obtainMessage(MAKE_GUESS_AND_SEND));
                            countPlayer2++;
                        }
                    }
                    //Both the players exhausted the number of guesses limit. We can quit it safely
                    else if (countPlayer1 >= NO_OF_GUESSES && countPlayer2 >= NO_OF_GUESSES && mThread1.getLooper() != null && mThread2.getLooper() != null) {
                        mThread1.executeLooperQuitSafely();
                        mThread2.executeLooperQuitSafely();
                        postGameStatus("It's a DRAW!!!");
                    }
                    Log.i(TAG, "After countPlayer1:" + countPlayer1 + "\tcountPlayer2:" + countPlayer2);
                    break;

                default:
                    break;
            }
        }
    };
    FirstPlayer mThread1;
    SecondPlayer mThread2;

    private int countPlayer1;
    private int countPlayer2;

    private final String THREAD_NAME_PLAYER_1 = "player1";
    private final String THREAD_NAME_PLAYER_2 = "player2";

    private final int MAKE_GUESS_AND_SEND = 1;
    private final int RESPOND_OTHER_PLAYER_GUESS = 2;
    private final int RECEIVE_RESPONSE = 3;
    private final int CHECK_STATUS_PLAYER = 4;

    // To change the limit for the maximum number of guesses
    private final int NO_OF_GUESSES = 20;
    private int WAIT_TIME = 1000;

    //created for a game where a player cheats and win. Change the visibility of the button in the XML file from 'gone' to 'visible'
    private boolean winGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.text);
        textView2 = (TextView) findViewById(R.id.text2);
        textView3 = (TextView) findViewById(R.id.text3);
        btnStart = (Button) findViewById(R.id.btn_start);
        btnStartWin = (Button) findViewById(R.id.btn_start_win);

        // Click event for cheating and winning
        btnStartWin.setOnClickListener(new View.OnClickListener() {
            /**
             * Called when a view has been clicked.
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                winGame = true;
                commonButtonCode();
            }
        });
        btnStart.setOnClickListener(new View.OnClickListener() {
            /** Called when a view has been clicked. @param v The view that was clicked. */
            @Override
            public void onClick(View v) {
                winGame = false;
                commonButtonCode();

            }
        });
    }

    /**
     * Code to run on each button click to run the game.
     */
    private void commonButtonCode(){

        // resetting the count of the players
        countPlayer1 = 0;
        countPlayer2 = 0;
        // empty the text area
        postGameStatus("");

        //if the thread is already running then QUIT the Looper
        if(mThread1!=null && mThread1.getLooper()!=null){
            mThread1.executeLooperQuit();
        }
        if(mThread2!=null && mThread2.getLooper()!=null){
            mThread2.executeLooperQuit();
        }

        //Instantiate 2 new threads
        mThread1 = new FirstPlayer(THREAD_NAME_PLAYER_1);
        mThread2 = new SecondPlayer(THREAD_NAME_PLAYER_2);

        //Start the 2 threads
        mThread1.start();
        mThread2.start();

        /*waitSomeTime(500);*/

        //Send a message to UIHandler to see who can make a move
        mThread1.sendMessageToUi();
        mThread2.sendMessageToUi();
    }


    /**
     * Make the thread go to sleep for a specific amount of time
     * @param ms
     */
    private void waitSomeTime(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Post a Runnable to the UIHandler for updating the TextView
     * @param newText
     */
    private void postGameStatus(final String newText) {
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (textView3) {
                    textView3.setText(newText);
                }
            }
        });
    }

    /* Worker thread*/
    public class FirstPlayer extends HandlerThread {

        private String TAG = "FirstPlayer";
        private String myNumber;
        private String guess;
        private String otherPlayerGuess;
        private String responseToGuess;
        private String responseToOtherPlayerGuess;
        private int[]  responseToOtherPlayerGuessDigits=new int[]{0,0};

        private int count = 1;

        private boolean madeGuess;
        private boolean respondedToGuess;

        private String name;

        private Handler firstPlayerHandler;

        /**
         * Post a Runnable to the UIHandler for updating the TextView
         * @param newText
         */
        private void postToUi(final String newText) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (textView) {
                        String temp = textView.getText().toString();
                        textView.setText(temp + "\n" + newText);
                    }
                }
            });
        }

        /**
         * Sending message to the UI Thread for checking the status.
         */
        private void sendMessageToUi() {
            Message msg = mUiHandler.obtainMessage(CHECK_STATUS_PLAYER);
            Bundle bundle=new Bundle();
            bundle.putString("NAME",THREAD_NAME_PLAYER_1);
            bundle.putIntArray("GUESSES",responseToOtherPlayerGuessDigits);
            msg.setData(bundle);
            mUiHandler.sendMessage(msg);
        }

        /**
         * Returns the reference of the handler. If the handler is not created from the onLooperPrepare() method then we wait till it is created.
         * We want to avoid a null handler reference to the other player.
         * @return {@link Handler} reference
         */
        public Handler getFirstPlayerHandler() {
            synchronized (this) {
                while (isAlive() && firstPlayerHandler == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            return firstPlayerHandler;
        }

        /**
         * Constructor for the player. Thread priority is set so as to make it run at a lower priority then the UI thread.
         * @param name
         */
        public FirstPlayer(String name) {
            super(name, THREAD_PRIORITY_BACKGROUND);
            this.name = name;
        }

        /**
         * Once the looper's prepare() method is called, some work needs to be done.
         * 1. The handler is created.
         * 2. Also anyone waiting on the handler instance to be created will be notified.
         * 3. Secret Number is guesses here.
         * 4. Number is posted to UI
         */
        @Override
        public void onLooperPrepared() {
            Log.i(TAG, name + "name");
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (textView) {
                        textView.setText("");
                    }
                }
            });
            firstPlayerHandler = new Handler(getLooper()) {
                /**
                 * Subclasses must implement this to receive messages.
                 * @param msg
                 */
                @Override
                public void handleMessage(Message msg) {
                    Message tempMsg;
                    int what = msg.what;
                    switch (what) {
                        //Make a strategic guess and send a message for a response from the 2nd player
                        case MAKE_GUESS_AND_SEND:
                            guess = GuessUtility.guessStrategyForFirst();
                            Log.i(TAG, count + "guess :" + guess);
                            madeGuess = true;
                            waitSomeTime(WAIT_TIME);
                            tempMsg = mThread2.getSecondPlayerHandler().obtainMessage(RESPOND_OTHER_PLAYER_GUESS);
                            tempMsg.obj = guess;
                            mThread2.getSecondPlayerHandler().sendMessage(tempMsg);
                            break;

                        //Receive guess from the 2nd player and send a message with the response to the same player.
                        case RESPOND_OTHER_PLAYER_GUESS:
                            otherPlayerGuess = (String) msg.obj;

                            tempMsg = mThread2.getSecondPlayerHandler().obtainMessage(RECEIVE_RESPONSE);
                            responseToOtherPlayerGuessDigits=GuessUtility.matchGuess(myNumber, otherPlayerGuess, winGame);
                            responseToOtherPlayerGuess = GuessUtility.getStringMatchGuess(responseToOtherPlayerGuessDigits);
                            tempMsg.obj = responseToOtherPlayerGuess;
                            //send message for the guess to the other player which will be received
                            mThread2.getSecondPlayerHandler().sendMessage(tempMsg);
                            respondedToGuess = true;
                            break;

                        // Receiving the response from the 2nd player
                        case RECEIVE_RESPONSE:
                            responseToGuess = (String) msg.obj;
                            if (respondedToGuess && madeGuess) {
                                postToUi("-------------------------------------------------\nNo.:" + count + " Guess : " + otherPlayerGuess + " \nResponse: " + responseToOtherPlayerGuess);
                                //send check status message to UI thread handler
                                sendMessageToUi();
                                madeGuess = false;
                                respondedToGuess = false;
                                count++;
                                Log.i(TAG, count + "");
                            }
                            break;
                        default:
                            break;
                    }
                }
            };
            //notify those objects waiting on the handler instance
            synchronized (this) {
                notifyAll();
            }
            // make a random guess
            myNumber = GuessUtility.getValidGuess();
            //myNumber = "1056";

            // post the number to the UI
            postToUi("Player 1's number: " + myNumber);
        }

        /**
         * Inform the Looper to QuitSafely
         */
        public void executeLooperQuitSafely() {
            this.getLooper().quitSafely();
        }

        /**
         * Inform the Looper to Quit even if there are pending messages in the queue.
         */
        public void executeLooperQuit() {
            this.getLooper().quit();
        }
    }

    // Worker thread
    public class SecondPlayer extends HandlerThread {
        private String TAG = "SecondPlayer";
        private String myNumber;
        private String guess;
        private String otherPlayerGuess;
        private String responseToGuess;
        private String responseToOtherPlayerGuess;
        private int[]  responseToOtherPlayerGuessDigits=new int[]{0,0};

        private int count = 1;

        private boolean madeGuess;
        private boolean respondedToGuess;

        private String name;
        private Handler secondPlayerHandler;

        /**
         * Post a Runnable to the UIHandler for updating the TextView
         * @param newText
         */
        private void postToUi(final String newText) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (textView2) {
                        String temp = textView2.getText().toString();
                        textView2.setText(temp + "\n" + newText);
                    }
                }
            });
        }

        /**
         * Sending message to the UI Thread for checking the status.
         */
        private void sendMessageToUi() {
            Message msg = mUiHandler.obtainMessage(CHECK_STATUS_PLAYER);
            Bundle bundle =new Bundle();
            bundle.putString("NAME",THREAD_NAME_PLAYER_2);
            bundle.putIntArray("GUESSES",responseToOtherPlayerGuessDigits);
            msg.setData(bundle);
            mUiHandler.sendMessage(msg);
        }

        /**
         * Returns the reference of the handler. If the handler is not created from the onLooperPrepare() method then we wait till it is created.
         * We want to avoid a null handler reference to the other player.
         * @return {@link Handler} reference
         */
        public Handler getSecondPlayerHandler() {
            synchronized (this) {
                while (isAlive() && secondPlayerHandler == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            return secondPlayerHandler;
        }

        /**
         * Constructor for the player. Thread priority is set so as to make it run at a lower priority then the UI thread.
         * @param name
         */
        public SecondPlayer(String name) {
            super(name, THREAD_PRIORITY_BACKGROUND);
            this.name = name;
        }

        /**
         * Once the looper's prepare() method is called, some work needs to be done.
         * 1. The handler is created.
         * 2. Also anyone waiting on the handler instance to be created will be notified.
         * 3. Secret Number is guesses here.
         * 4. Number is posted to UI
         */
        public void onLooperPrepared() {
            Log.i(TAG, name + "name");
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (textView2) {
                        textView2.setText("");
                    }
                }
            });
            secondPlayerHandler = new Handler(getLooper()) {
                /**
                 * Subclasses must implement this to receive messages.
                 *
                 * @param msg
                 */
                @Override
                public void handleMessage(Message msg) {
                    Message tempMsg;
                    int what = msg.what;
                    switch (what) {
                        //Make a strategic guess and send a message for a response from the 2nd player
                        case MAKE_GUESS_AND_SEND:
                            guess = GuessUtility.getValidGuess();
                            Log.i(TAG, count + "guess :" + guess);
                            madeGuess = true;
                            waitSomeTime(WAIT_TIME);
                            tempMsg = mThread1.getFirstPlayerHandler().obtainMessage(RESPOND_OTHER_PLAYER_GUESS);
                            tempMsg.obj = guess;
                            mThread1.getFirstPlayerHandler().sendMessage(tempMsg);
                            break;

                        //Receive guess from the 2nd player and send a message with the response to the same player.
                        case RESPOND_OTHER_PLAYER_GUESS:
                            otherPlayerGuess = (String) msg.obj;

                            tempMsg = mThread1.getFirstPlayerHandler().obtainMessage(RECEIVE_RESPONSE);
                            responseToOtherPlayerGuessDigits=GuessUtility.matchGuess(myNumber, otherPlayerGuess,winGame);
                            responseToOtherPlayerGuess = GuessUtility.getStringMatchGuess(responseToOtherPlayerGuessDigits);
                            tempMsg.obj = responseToOtherPlayerGuess;
                            //send message for the guess to the other player which will be received
                            mThread1.getFirstPlayerHandler().sendMessage(tempMsg);
                            respondedToGuess = true;
                            break;

                        // Receiving the response from the 2nd player
                        case RECEIVE_RESPONSE:
                            responseToGuess = (String) msg.obj;
                            if (respondedToGuess && madeGuess) {
                                postToUi("-------------------------------------------------\nNo.:" + count + " Guess : " + otherPlayerGuess + " \nResponse: " + responseToOtherPlayerGuess);
                                //send check status message to UI thread handler
                                sendMessageToUi();
                                madeGuess = false;
                                respondedToGuess = false;
                                count++;
                                Log.i(TAG, count + "");
                            }
                            break;
                        default:
                            break;
                    }
                }
            };
            //notify those objects waiting on the handler instance
            synchronized (this) {
                notifyAll();
            }
            // make a random guess
            myNumber = GuessUtility.getValidGuess();
            //myNumber = "1042";

            // post the number to the UI
            postToUi("Player 2's number: " + myNumber);
        }

        /**
         * Inform the Looper to QuitSafely
         */
        public void executeLooperQuitSafely() {
            this.getLooper().quitSafely();
        }

        /**
         * Inform the Looper to Quit even if there are pending messages in the queue.
         */
        public void executeLooperQuit() {
            this.getLooper().quit();
        }
    }
}