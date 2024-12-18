package com.example.exercisetracker.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.exercisetracker.R;
import com.example.exercisetracker.other.DBhelper;
import com.example.exercisetracker.other.Friend;
import com.example.exercisetracker.other.User;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Objects;

public class AddFriendsActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText searchEditText;

    //recycler view
    private AddFriendsActivity.FriendAdapter courseAdapter;
    private ArrayList<Friend> friendArr;
    private TextView noFriends;

    //loading dialog
    private ProgressDialog loadingDialog;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //handling when back button is pressed
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //visuals
        setContentView(R.layout.activity_addfriends);
        // showing the back button in action bar
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        getWindow().setStatusBarColor(getResources().getColor(R.color.main_colour));

        //setting on click listener to class
        findViewById(R.id.searchBtn).setOnClickListener(this);
        TextInputLayout input = findViewById(R.id.searchView);
        searchEditText = input.getEditText();
        noFriends = findViewById(R.id.noFriendsTV);

        //recycler views
        RecyclerView recyclerView = findViewById(R.id.friendsRV);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        friendArr = new ArrayList<>();
        // we are initializing our adapter class and passing our arraylist to it.
        courseAdapter = new AddFriendsActivity.FriendAdapter(getApplicationContext(), friendArr);
        recyclerView.setAdapter(courseAdapter);
        recyclerView.setLayoutManager(linearLayoutManager);

        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Loading..");
        loadingDialog.setTitle("Retrieving Your Friends List");
        loadingDialog.setIndeterminate(true);
        loadingDialog.setCancelable(false);
        loadingDialog.show();
        //by default, the user's friends list shows on the screen
        //they can decide to remove any of their friends
        new LoadFriendsListTask().execute(true);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.searchBtn) {//refreshing the static user class friend list from database
            DBhelper helper = new DBhelper(this);
            //when user wants to search for other users
            if (!searchEditText.getText().toString().equals("")) {
                helper.clearResults();
                if (helper.getUsers(searchEditText.getText().toString())) {
                    if (!helper.getResult().isEmpty()) {
                        noFriends.setVisibility(View.INVISIBLE);
                        //list of users passed to recycler view
                        //resetting friendArr for new query
                        friendArr.clear();
                        courseAdapter.notifyDataSetChanged();
                        for (String row : helper.getResult()) {
                            Friend friendObj = handleQuery(row);
                            friendArr.add(friendObj);
                            courseAdapter.notifyItemInserted(courseAdapter.getItemCount());
                            // setting layout manager and adapter to our recycler view.
                        }
                    } else {
                        //no users found from result
                        Toast.makeText(getApplicationContext(), "No Users Found", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                //user has searched empty query
                Toast.makeText(getApplicationContext(), "You have not entered anything", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Friend handleQuery(String query) {
        String[] arr = query.split(" ");
        int id = Integer.parseInt(arr[0]);
        String firstname = arr[1];
        String surname = arr[2];
        String username = arr[3];
        return new Friend(id, firstname, surname, username);
    }

    //adapter class to exchange information between card views created and friend details
    public static class FriendAdapter extends RecyclerView.Adapter<AddFriendsActivity.FriendAdapter.Viewholder> {
        private final Context context;
        private final ArrayList<Friend> friendsArr;

        public FriendAdapter(Context context, ArrayList<Friend> friendsArr) {
            this.context = context;
            this.friendsArr = friendsArr;

        }

        @NonNull
        @Override
        public AddFriendsActivity.FriendAdapter.Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            //inflate the layout for each item of recycler view.
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friends_card_layout, parent, false);
            return new AddFriendsActivity.FriendAdapter.Viewholder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AddFriendsActivity.FriendAdapter.Viewholder holder, int position) {
            //setting the instance holder data from the friend object at that position in the array
            Friend friend = friendsArr.get(position);

            if (User.getFriendsList().contains(friend.getId())) {
                //if friend is in the user's friend list change button to remove friend
                holder.addFriendBtn.setText("Remove Friend");
            } else {
                holder.addFriendBtn.setText("Add Friend");
            }


            holder.realNameTV.setText(friend.getFirstname() + " " + friend.getSurname());
            holder.usernameIDTV.setText("username: " + friend.getUsername());
            holder.addFriendBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (holder.addFriendBtn.getText().equals("Add Friend")) {
                        //handling when user clicks on add friend on a specific user
                        DBhelper helper = new DBhelper(context.getApplicationContext());
                        if (helper.addFriend(User.getUserID(), friend.getId())) {
                            Toast.makeText(context.getApplicationContext(), "Friend Added", Toast.LENGTH_SHORT).show();
                            holder.addFriendBtn.setText("Remove Friend");
                        } else {
                            Toast.makeText(context.getApplicationContext(), "Could not add friend", Toast.LENGTH_SHORT).show();
                        }
                    } else if (holder.addFriendBtn.getText() == "Remove Friend") {
                        //when friend has been already added and wants to remove friendship
                        DBhelper helper = new DBhelper(context);
                        if (helper.removeFriend(User.getUserID(), friend.getId())) {
                            Toast.makeText(context.getApplicationContext(), "Friend Removed", Toast.LENGTH_SHORT).show();
                            holder.addFriendBtn.setText("Add Friend");
                        } else {
                            Toast.makeText(context.getApplicationContext(), "Could not remove friend", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return friendsArr.size();
        }

        // View holder class for initializing of views such as TextView
        public static class Viewholder extends RecyclerView.ViewHolder {
            private final TextView realNameTV;
            private final TextView usernameIDTV;
            private final Button addFriendBtn;

            public Viewholder(@NonNull View itemView) {
                super(itemView);
                //binding views to instance of Viewholder
                realNameTV = itemView.findViewById(R.id.userRealName);
                usernameIDTV = itemView.findViewById(R.id.usernameID);
                addFriendBtn = itemView.findViewById(R.id.addFriendBtn);
            }
        }
    }

    private class LoadFriendsListTask extends AsyncTask<Boolean, Integer, ArrayList<String>> {
        //task to load friends list
        protected ArrayList<String> doInBackground(Boolean... isPublic) {
            //retrieve data from database
            DBhelper helper = new DBhelper(getApplicationContext());
            if (helper.getFriends()) {
                return helper.getResult();
            }
            return null;
        }

        protected void onPostExecute(ArrayList<String> result) {
            loadingDialog.dismiss();
            if (result == null) {
                //no friends found due to connection issue
                noFriends.setVisibility(View.VISIBLE);
            } else if (!result.isEmpty()) {
                noFriends.setVisibility(View.INVISIBLE);
                User.clearFriendsList();
                friendArr.clear();
                courseAdapter.notifyDataSetChanged();
                for (String query : result) {
                    Friend friendObj = handleQuery(query);
                    //adding to user's list of friends
                    User.addFriendsList(friendObj.getId());
                    friendArr.add(friendObj);
                    courseAdapter.notifyItemInserted(courseAdapter.getItemCount());
                    //adding to recycler view (by default when user loads this section
                    //their friends will appear
                }
            } else {
                //no friends found, disclaimer shown to user
                noFriends.setVisibility(View.VISIBLE);
            }
        }
    }
}


