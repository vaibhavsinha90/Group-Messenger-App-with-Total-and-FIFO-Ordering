package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.regex.Pattern;

public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerProvider.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";//54
    static final String REMOTE_PORT1 = "11112";//56
    static final String REMOTE_PORT2 = "11116";//58
    static final String REMOTE_PORT3 = "11120";//60
    static final String REMOTE_PORT4 = "11124";//62
    static final int SERVER_PORT = 10000;
    static Integer DevId;
    static ConcurrentHashMap<String,Hold> HT=new ConcurrentHashMap<String, Hold>();
    static Integer seqnum=0,maxprop=0;
    static Double prop=0.0, IAmax=0.0;
    static Integer seq=0;
    static String crash="NONE";
    
    static ConcurrentHashMap<String,String> htable = new ConcurrentHashMap<String, String>();
    static PriorityBlockingQueue<QueueEntry> undel=new PriorityBlockingQueue<QueueEntry>();
    static PriorityBlockingQueue<QueueEntry> temp=new PriorityBlockingQueue<QueueEntry>();
    static PriorityBlockingQueue<QueueEntry> temp2=new PriorityBlockingQueue<QueueEntry>();
    static PriorityBlockingQueue<QueueEntry> undel2=new PriorityBlockingQueue<QueueEntry>();
    static HashMap<String, Timer> timers = new HashMap<String, Timer>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        final String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        DevId=Integer.parseInt(portStr)-5500;
        Log.i(TAG, "DevId: "+DevId.toString());

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String msg = editText.getText().toString();
                editText.setText(""); 
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.setMovementMethod(new ScrollingMovementMethod());
                localTextView.append("\tSent:" + msg); 

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "1", msg, myPort);
            }
        });
    }
    public class QueueEntry implements Comparable<QueueEntry>
    {
        private String msg;
        private Integer src;
        private Integer src_seq;
        private Double A_seq;
        private boolean deliverable;

        public String getmsg()
        {
            return msg;
        }
        public Integer getsrc()
        {
            return src;
        }
        public Integer getsrc_s()
        {
            return src_seq;
        }
        public Double getAS()
        {
            return A_seq;
        }
        public void setAS(Double as){this.A_seq=as;}
        public void setDel(boolean d)
        {
            this.deliverable=d;
        }
        public boolean getDel()
        {
            return deliverable;
        }

        QueueEntry(String m, Integer dev, Integer src_s, Double AS)
        {
            msg=m;
            src=dev;
            src_seq=src_s;
            A_seq=AS;
            deliverable=false;
        }

        QueueEntry(String ms)
        {
            String part[]=ms.split(Pattern.quote("**"));
            msg=part[0];
            String srcstuff[]=part[1].split(",");
            src=Integer.parseInt(srcstuff[0]);
            src_seq=Integer.parseInt(srcstuff[1]);
            A_seq=0.0;
            deliverable=false;
        }
        @Override
        public int compareTo(QueueEntry q2) {
            Double res = q2.getAS();
            Double diff=(this.A_seq - res);
            if(diff>0)
                return 1;
            else {
                if (diff == 0)
                    return 0;
                else
                    return -1;
            }
        }
    }
    public class QueueHolding implements Comparable<QueueHolding>
    {
        private String msg;
        private Integer src;
        private Integer src_seq;
        private Double A_seq;
        private boolean deliverable;

        public String getmsg()
        {
            return msg;
        }
        public Integer getsrc()
        {
            return src;
        }
        public Integer getsrc_s()
        {
            return src_seq;
        }
        public Double getAS()
        {
            return A_seq;
        }
        public void setAS(Double as){this.A_seq=as;}
        public void setDel(boolean d)
        {
            this.deliverable=d;
        }
        public boolean getDel()
        {
            return deliverable;
        }
        QueueHolding(String m, Integer dev, Integer src_s, Double AS)
        {
            msg=m;
            src=dev;
            src_seq=src_s;
            A_seq=AS;
            deliverable=false;
        }

        QueueHolding(String msg)
        {
            String part[]=msg.split(Pattern.quote("**"));
            msg=part[0];
            String srcstuff[]=part[1].split(",");
            src=Integer.parseInt(srcstuff[0]);
            src_seq=Integer.parseInt(srcstuff[1]);
            A_seq=0.0;
            deliverable=false;
        }
        @Override
        public int compareTo(QueueHolding q2) {
            Integer res = q2.getsrc_s();
            Integer diff=(this.src_seq - res);
            if(diff>0)
                return 1;
            else {
                if (diff == 0)
                    return 0;
                else
                    return -1;
            }
        }
    }
    public class Hold
    {
        private Integer LastSeqDeliv;
        private PriorityBlockingQueue<QueueHolding> holding=new PriorityBlockingQueue<QueueHolding>();

        public Integer getls()
        {
            return LastSeqDeliv;
        }
        public void setls(Integer s)
        {
            this.LastSeqDeliv=s;
        }
        public PriorityBlockingQueue<QueueHolding> getHL()
        {
            return holding;
        }
        Hold(Integer ls)
        {
            LastSeqDeliv=ls;
            //holding.add(q);
        }
        public void add2holding (QueueHolding q)
        {
            this.holding.add(q);
        }
    }
    protected void delivery_done(String m, Integer src, Integer src_seq )
    {
        ContentValues keyValueToInsert = new ContentValues();
        keyValueToInsert.put("key", (seqnum++).toString());
        keyValueToInsert.put("value", m);
        Uri providerUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
        Uri newUri = getContentResolver().insert(providerUri,keyValueToInsert);
        //seqnum=seqnum+1;
    }
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    protected synchronized void fifo (String who)
    {
        while(undel.size()>0 && undel.peek().getDel()) {
            //ready for delivery so check fifo now
            try {
                QueueEntry head = undel.poll();

                if (HT.containsKey(head.getsrc().toString())) {
                    Hold h = HT.get(head.getsrc().toString());
                    if (h.getls() == head.getsrc_s() - 1) {
                        
                            //deliver and update HashTable
                            delivery_done(head.getmsg(), head.getsrc(), head.getsrc_s());
                            Log.i(TAG, who+" FIFO'd:" + head.getsrc() + "," + head.getsrc_s());
                            h.setls(h.getls() + 1);
                            int ls = h.getls();
                            while (h.holding.size() > 0) {
                                if (h.holding.peek().getsrc_s() == (ls + 1)) {
                                    QueueHolding nexthold = h.holding.poll();
                                    delivery_done(nexthold.getmsg(), nexthold.getsrc(), nexthold.getsrc_s());
                                    Log.i(TAG, who+" FIFO'dEx:" + nexthold.getsrc() + "," + nexthold.getsrc_s());
                                    ls++;
                                    h.setls(ls);
                                } else {
                                    break;
                                }
                            }
                            HT.put(head.getsrc().toString(), h);
                        
                    } else {
                        //hold the msg!
                       
                            QueueHolding newhold = new QueueHolding(head.getmsg(), head.getsrc(), head.getsrc_s(), head.getAS());
                            newhold.setDel(head.getDel());  //although it's certain that head.getDel will return 'true'
                            h.add2holding(newhold);
                            HT.put(head.getsrc().toString(), h);
                            Log.i(TAG, who + " Holding " + head.getsrc() + "," + head.getsrc_s());
                    
                    }
                } else {
                    
                        //deliver and update HashTable. This was the 1st msg from some avd
                        delivery_done(head.getmsg(), head.getsrc(), head.getsrc_s());
                        Hold nH = new Hold(0);
                        HT.put(head.getsrc().toString(), nH);
                        Log.i(TAG, who+" 1st delivery by " + head.getsrc());
                    

                }

            } catch (NullPointerException e) {
                Log.e(TAG, e + "NPE queue issue: 3) adding msg to del");
            }
        }
    }


    protected void inform_others(String crashed)
    {
        String remotePort = REMOTE_PORT0;
        String dev="";
        for(int rp=0;rp<5;rp++)
        {
            switch (rp)
            {
                case 0:
                    remotePort = REMOTE_PORT0;
                    dev="54";
                    break;
                case 1:
                    remotePort = REMOTE_PORT1;
                    dev="56";
                    break;
                case 2:
                    remotePort = REMOTE_PORT2;
                    dev="58";
                    break;
                case 3:
                    remotePort = REMOTE_PORT3;
                    dev="60";
                    break;
                case 4:
                    remotePort = REMOTE_PORT4;
                    dev="62";
                    break;
            }
            if(dev.equals(crashed) || dev.equals(DevId.toString()))
                continue;

            try
            {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(remotePort));
                BufferedWriter w = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                Log.i(TAG,"Informing!!"+" by "+DevId+" to "+dev);
                w.write("CRD:" + crashed + "\n");
                w.flush();
                w.close();
                socket.close();
            }
            catch (UnknownHostException e) {
                Log.e(TAG, "Informing err!"+e);
            } catch (IOException e) {
                Log.e(TAG, "Informing err!"+e);
            }
        }

    }

    private class PQupdate extends TimerTask
    {
        String avd;
        public PQupdate(String dev)
        {
            avd = dev;
        }

        @Override
        public void run() {
            Iterator<QueueEntry> i=undel.iterator();
            temp2.clear();
            int rem=0;
            while(i.hasNext())
            {
                QueueEntry qe= i.next();

                
                if(avd.equals(qe.src.toString()) && qe.getDel()==false) //checking if MsgID (eg: 54,1) exists
                {
                    //do nothing
                    Log.i(TAG,"Timer removed "+qe.getsrc()+","+qe.getsrc_s());
                    rem=1;
                }
                else
                {
                    temp2.add(qe);
                }
            }
            if(rem==1) {
                undel.clear();
                while (temp2.size() > 0) {
                    undel.add(temp2.poll());
                }
                fifo("Timer");
            }

        }
    }
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            try
            {
                while (true) {
                    Socket s = serverSocket.accept();
                    s.setSoTimeout(2500);
                    BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    String inp=r.readLine();
                    BufferedWriter wss = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                    if(inp.substring(0, 4).equals("I,A:")) // case 3: recvd agreed seq
                    {
                        wss.write("rIA:" + inp + "\n");
                        wss.flush();
                    }
                    String ess[] = ProcessMsg(inp);



                    String msgToSend = ess[1];
                    if (ess[0].equals("2"))
                    {
                        String origin[] = ess[2].split(",");
                        Log.i(TAG, "step 2! sent to:" + origin[0] + "when" + ess[2] + "*" + msgToSend);
                        wss.write("I,S:" + ess[2] + "**" + msgToSend + "\n");
                        wss.flush();
                        String ack;
                        try {
                            ack = r.readLine();
                            if(ack==null)
                            {
                                Log.i(TAG,"Ack==null");
                                throw new SocketTimeoutException();
                            }
                            Log.i(TAG,"Rep of I,S:"+ack);
                        }
                        catch(SocketTimeoutException t)
                        {
                            ack="No ACK!";
                            Log.i(TAG,"TimedOut! Rep of I,S:"+ack);
                        }

                        Log.i(TAG, "ess=" + ess[0] + "," + ess[1]+","+ess[2]);
                        if(!ack.equals("ACK:" + ess[2]))
                        {
                            Log.i(TAG,"ACK NOT RECVD! Will remove the undelivered msgs from the avd in context!");
                            
                            Iterator<QueueEntry> i=undel.iterator();
                            temp.clear();
                            int rem2=0;
                            while(i.hasNext())
                            {
                                QueueEntry qe= i.next();

                                
                                if(qe.src==Integer.parseInt(origin[0]) && qe.getDel()==false) //checking if MsgID (eg: 54,1) exists
                                {
                                    //do nothing
                                    Log.i(TAG,"removed "+qe.getsrc()+","+qe.getsrc_s());
                                    rem2=1;
                                    Log.i(TAG,qe.getsrc().toString()+" crashed! Old val of crash:"+crash);
                                    if(crash.equals("NONE"))
                                    {
                                        Log.i(TAG, "Timer On! @No ack");
                                        Timer time = new Timer();
                                        time.schedule(new PQupdate(qe.getsrc().toString()), 3500,500);
                                        inform_others(qe.getsrc().toString());
                                    }
                                    crash=qe.getsrc().toString();
                                }
                                else
                                {
                                    temp.add(qe);
                                }
                            }
                            if(rem2==1)
                            {
                                undel.clear();
                                while (temp.size() > 0) {
                                    undel.add(temp.poll());
                                }
                                fifo("NoAck");
                            }
                        }
                    }

                    s.close();


                }

            }
            catch (IOException e) {
                Log.e(TAG, "socket/DiB issue"+e);
            }

            return null;
        }


        protected String[] ProcessMsg(String...strings) {

            Log.i(TAG, "to process: " + strings[0]);
            if(strings[0].substring(0, 4).equals("M,I:"))
            {
                String strReceived = strings[0].substring(4).trim();
                try {
                    QueueEntry qe = new QueueEntry(strReceived);
                    Log.i(TAG, qe.getmsg() + "," + qe.getsrc() + "," + qe.getsrc_s() + ": added to undel");
                    undel.add(qe);

                    if(qe.getsrc()!=DevId)
                    {
                        String ID = qe.getsrc().toString() + "," + qe.getsrc_s().toString();
                        Timer time = new Timer();
                        time.schedule(new MsgIA(ID), 7990);
                        timers.put(ID, time);
                    }
                    
                    prop=maxprop>seqnum? (double)maxprop:(double)seqnum;
                    prop=prop>IAmax? prop:IAmax;
                    prop++;
                    maxprop=prop.intValue();

                    String prop2s= ((Integer)prop.intValue()).toString();
                    prop2s=prop2s+"."+DevId.toString();

                    String msg1[]=strReceived.split(Pattern.quote("**"));
                    String ret[]={"2", prop2s,msg1[1]};
                    return ret;

                    //proposed seqnum
                }
                catch (ClassCastException e)
                {
                    Log.e(TAG, "IE queue issue: 1) adding msg ");
                }
                catch (NullPointerException e)
                {
                    Log.e(TAG, "NPE queue issue: 1) adding msg ");
                }
            }
            if(strings[0].substring(0, 4).equals("I,A:")) // case 3: recvd agreed seq
            {
                String strReceived = strings[0].substring(4).trim();
                String msg1[]=strReceived.split(Pattern.quote("**"));
                if(IAmax<Double.parseDouble(msg1[1]))
                {
                    IAmax=Double.parseDouble(msg1[1]);
                }
                String vals[]=msg1[0].split(",");

                Iterator<QueueEntry> i=undel.iterator();
                undel2.clear();
                if(undel.size()>0)
                {
                    QueueEntry juy=undel.peek();
                    Log.i(TAG,"undel size: "+undel.size()+" | HEAD: "+juy.getsrc()+","+juy.getsrc_s());
                }
                else
                {
                    Log.i(TAG,"undel empty?!");
                }
                while(i.hasNext())
                {
                    QueueEntry qe= i.next();
                    if(qe.src==Integer.parseInt(vals[0]) && qe.src_seq==Integer.parseInt(vals[1])) //checking if MsgID (eg: 54,1) exists
                    {
                        QueueEntry qe2= new QueueEntry(qe.getmsg(),qe.getsrc(),qe.getsrc_s(),Double.parseDouble(msg1[1]));
                        qe2.setDel(true);
                        undel2.add(qe2);
                        if(qe.src!=DevId) {
                            Timer t = timers.get(qe.getsrc().toString() + "," + qe.getsrc_s().toString());
                            t.cancel();
                        }
                    }
                    else
                    {
                        undel2.add(qe);
                    }
                }
                undel.clear();
                while(undel2.size()>0)
                {
                    undel.add(undel2.poll());
                }

                Log.i(TAG,"updated recvd AS, now checking deliverability!");
                //updated AS, priority Queue gets rearranged on its own i.e. lowest one comes at 'head'
                if(undel.size()>0 && undel.peek().getDel()) {
                    fifo("IA");
                }

                String ret[]={"0", "internal msg for 3","internal msg for 3"};
                return ret;
            }
            if(strings[0].substring(0, 4).equals("CRD:"))
            {
                if(crash.equals("NONE")) {
                    Log.i(TAG, "By infroming, Timer On!");
                    crash=strings[0].substring(4).trim();
                    Timer time = new Timer();
                    time.schedule(new PQupdate(crash), 5500, 1000);
                }

            }
            String ret[]={"0","Shall never be sent","Just for syntax"};
            return ret;
        }

    }
    private class MsgIA extends TimerTask
    {
        String id;
        public MsgIA(String Mid)
        {
            id = Mid;
        }

        @Override
        public void run() {
            Iterator<QueueEntry> itr=undel.iterator();

            while(itr.hasNext())
            {
                QueueEntry qe= itr.next();

                if(id.equals(qe.getsrc().toString() + "," + qe.getsrc_s().toString()) && qe.getDel()==false) //checking if MsgID (eg: 54,1) exists
                {
                    //do nothing
                    Log.i(TAG,"MsgIA says: "+qe.getsrc()+":"+id+" crashed! Old val of crash:"+crash);
                    if(crash.equals("NONE")) {
                        Log.i(TAG, "Timer On! by MsgIA");
                        Timer time = new Timer();
                        time.schedule(new PQupdate(qe.getsrc().toString()), 10, 1000);
                        inform_others(qe.getsrc().toString());

                    }
                    crash=qe.getsrc().toString();
                }
            }
        }
    }



    private class ClientTask extends AsyncTask<String, Void, Void> {
        protected void sendIA(String[] IAmsg)
        {
            String remotePort = REMOTE_PORT0;
            for(int rp=0;rp<5;rp++)
            {
                switch (rp)
                {
                    case 0:
                        remotePort = REMOTE_PORT0;
                        break;
                    case 1:
                        remotePort = REMOTE_PORT1;
                        break;
                    case 2:
                        remotePort = REMOTE_PORT2;
                        break;
                    case 3:
                        remotePort = REMOTE_PORT3;
                        break;
                    case 4:
                        remotePort = REMOTE_PORT4;
                        break;
                }
                try
                {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    BufferedWriter w = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    socket.setSoTimeout(1700);
                    w.write("I,A:" + IAmsg[2] + "**" + IAmsg[1] + "\n");
                    w.flush();
                    Log.i(TAG, "step 3!");

                    try {
                        String repIA = r.readLine();
                        if(repIA==null)
                        {
                            Log.i(TAG, "null reply for I,A from " + remotePort);
                            throw new SocketTimeoutException();
                        }
                        Log.i(TAG, "I,A's reply from " + remotePort + " " + repIA);
                        
                    }
                    catch (SocketTimeoutException e)
                    {
                        
                        Log.i(TAG, "I,A's reply from " + remotePort + " TimedOut!. "+e);
                    }
                    w.close();
                    socket.close();
                }
                catch (UnknownHostException e) {
                    Log.e(TAG, "Err in step 3, CT!"+e);
                } catch (IOException e) {
                    Log.e(TAG, "Err in step 3, CT!"+e);
                }
            }

        }


        protected String[] Proc_SR(String sr)
        {
            Log.i(TAG,"@CT to process: "+sr);
            if(sr.substring(0, 4).equals("NULL")) //recvd proposed seq
            {
                String Mid = sr.substring(4).trim();
                if(htable.containsKey(Mid))
                {
                    String vals[]=htable.get(Mid).split(",");
                    Integer count=Integer.parseInt(vals[1]);
                    count++;
                    htable.put(Mid, vals[0] + "," + count.toString());
                    Log.i(TAG, "htable stuff :" + Mid+"%"+vals[0]+","+count.toString());
                    if(count==4)
                    {
                        htable.remove(Mid);
                        String ret[]={"3", vals[0],Mid};
                        Log.i(TAG, "sending \"3\": "+Mid+"|"+vals[0]);
                        return ret;
                    }
                }
                else
                {
                    htable.put(Mid, "0,0");
                    String ret[]={"0", "internal msg for 2:init entry","internal msg for 2:init entry"};
                    return ret;
                }
            }
            if(sr.substring(0, 4).equals("I,S:")) //recvd proposed seq
            {
                String strReceived = sr.substring(4).trim();
                String msg2[]=strReceived.split(Pattern.quote("**"));
                if(htable.containsKey(msg2[0]))
                {
                    String vals[]=htable.get(msg2[0]).split(",");
                    Integer count=Integer.parseInt(vals[1]);
                    count++;

                    if(Double.parseDouble(vals[0])<Double.parseDouble(msg2[1]))
                    {
                        Log.i(TAG, vals[0]+" < "+msg2[1]);
                        vals[0]=msg2[1];
                    }
                    htable.put(msg2[0], vals[0] + "," + count.toString());
                    Log.i(TAG, "htable stuff :" + msg2[0]+"%"+vals[0]+","+count.toString());
                    if(count==4)
                    {
                        htable.remove(msg2[0]);
                        String ret[]={"3", vals[0],msg2[0]};
                        Log.i(TAG, "sending \"3\": "+msg2[0]+"|"+vals[0]);
                        return ret;
                    }
                }
                else
                {
                    htable.put(msg2[0], msg2[1] + ",0");
                    String ret[]={"0", "internal msg for 2:init entry","internal msg for 2:init entry"};
                    return ret;
                }
            }
            String ret[]={"0","checking MaxPS","checking MaxPS"};
            return ret;
        }

        @Override
        protected Void doInBackground(String... msgs) {
            Integer flag=1;
            try {
                String remotePort = REMOTE_PORT0;
                String dev="54";
                for(int rp=0;rp<5;rp++)
                {
                    switch(rp)
                    {
                        case 0:
                            remotePort = REMOTE_PORT0;
                            dev="54";
                            break;
                        case 1:
                            remotePort = REMOTE_PORT1;
                            dev="56";
                            break;
                        case 2:
                            remotePort = REMOTE_PORT2;
                            dev="58";
                            break;
                        case 3:
                            remotePort = REMOTE_PORT3;
                            dev="60";
                            break;
                        case 4:
                            remotePort = REMOTE_PORT4;
                            dev="62";
                            break;
                    }

                    String msgToSend = msgs[1];
                    String Mid=(DevId).toString()+","+seq.toString();
                    Log.i(TAG, "Mid: "+Mid);

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    BufferedWriter w = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    BufferedReader rc= new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    if(!dev.equals(DevId.toString()))
                        socket.setSoTimeout(2500); //1800 was doin well
                    w.write("M,I:" + msgToSend + "**" + Mid + "\n");
                    w.flush();
                    Log.i(TAG, "step 1!");
                    try {
                        String repIS = rc.readLine();
                        if(repIS==null)
                        {
                            Log.i(TAG,"null reply for M,I from " + remotePort);
                            throw new SocketTimeoutException();
                        }
                        Log.i(TAG, "M,I 's reply from " + remotePort + " " + repIS);
                        w.write("ACK:" + Mid + "\n");
                        w.flush();
                        flag=flag*10;
                        String r[]=Proc_SR(repIS);
                        if(r[0].equals("3")) {
                            w.close();
                            socket.close();
                            sendIA(r);
                        }
                        else {
                            w.close();
                            socket.close();
                        }
                    }
                    catch (SocketTimeoutException e)
                    {
                        //Log.e(TAG, "ClientTask UnknownHostException"+e);
                        Log.i(TAG, "M,I 's reply from " + remotePort + " TimedOut!. "+e);
                        String r[]=Proc_SR("NULL"+Mid);
                        if(r[0].equals("3"))
                        {
                            socket.close();
                            sendIA(r);
                        }
                        else
                            socket.close();
                        Log.i(TAG,dev+" crashed! Old val of crash:"+crash);
                        if(crash.equals("NONE")) {
                            Log.i(TAG, "@CTnoIS Timer On!");
                            Timer time = new Timer();
                            time.schedule(new PQupdate(dev), 5500, 1000);
                            inform_others(dev);

                        }
                        crash=dev;

                    }
                    
                }
                if(flag==100000)
                    seq++;
                else
                {seq++;Log.i(TAG,"Unexpected flag value! Sth Crashed, I guess.");}
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException"+e);
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException"+e);
            }

            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}

