package com.example.pet_care_final;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.WorkerThread;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class NodeMessageBroadcasterTask extends AsyncTask<Void, Void, Void> {

    protected final static String TAG = "pet_care_final";

    private Context _cx;
    private byte[] _msg;
    private String _path;

    public NodeMessageBroadcasterTask(Context cx, String path, byte[] msg )
    {
        this._cx = cx;
        this._msg = msg;
        this._path = path;
    }

    @Override
    protected Void doInBackground(Void... args) {
        Collection<String> nodes = getNodes();

        if ( nodes.isEmpty() ) {
            Log.d(TAG, "No connected nodes");
        }

        for (String node : nodes) {
            this.sendMessage(node);
        }
        return null;
    }

    @WorkerThread
    private void sendMessage(String node) {

        Log.d(TAG, "Sending message to " + node );

        Task<Integer> sendMessageTask =
                Wearable.getMessageClient( this._cx ).sendMessage(node, this._path, this._msg );

        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            Integer result = Tasks.await(sendMessageTask);
            Log.d(TAG, "Message sent: " + result);

        } catch (ExecutionException exception) {
            Log.e(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }
    }


    @WorkerThread
    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();

        Task<List<Node>> nodeListTask =
                Wearable.getNodeClient( this._cx ).getConnectedNodes();

        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            List<Node> nodes = Tasks.await(nodeListTask);

            for (Node node : nodes) {
                results.add(node.getId());
            }

        } catch (ExecutionException exception) {
            Log.e(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }

        return results;
    }
}
