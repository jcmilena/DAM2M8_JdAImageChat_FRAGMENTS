package com.example.jcmilena.jdaimagechat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements ImageChatFragment.OnImageChatListener, UserListFragment.OnUserListListener,
        ChatListFragment.OnChatListListener, LoginFragment.OnLoginListener, MessageFragment.OnMessageListener {


    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    FirebaseStorage cloud = FirebaseStorage.getInstance();
    Uri uri;
    ChildEventListener cargarChatsListener;
    List<MultimediaMsg> multimediaMsgList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        multimediaMsgList = new ArrayList<>();

        Fragment fragment = new LoginFragment();
        cargar_fragment(fragment,"LOGIN");
    }


    public void cargar_fragment(Fragment fragment, String TAG){

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, TAG).commit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Fragment fragment;
        switch (item.getItemId()){
            case R.id.newMsg:
                fragment = new ImageChatFragment();
                cargar_fragment(fragment, "IMAGECHAT");
                break;
            case R.id.logout:
                mAuth.signOut();
                fragment = new LoginFragment();
                cargar_fragment(fragment, "LOGIN");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void SendChat(final String missatge) {

        final UUID fotoname = UUID.randomUUID();
        UploadTask uploadTask;
        final StorageReference storage = cloud.getReference().child("images").child(fotoname+".jpg");
        uploadTask = storage.putFile(uri);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
            }
        });

        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return storage.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();

                    Fragment fragment = UserListFragment.newInstance(mAuth.getCurrentUser().getEmail()
                            ,downloadUri.toString(),missatge, fotoname.toString() );
                    cargar_fragment(fragment, "USERS");

                } else {
                    // Handle failures
                    // ...
                }
            }
        });




    }

    @Override
    public void CargarGaleria() {

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(intent, 10);

    }

    @Override
    public void pulsado(String uid, String fromEmail, String downloadurl, String message, String name ) {

        MultimediaMsg multimediaMsg = new MultimediaMsg(fromEmail, downloadurl, message, name);
        db.getReference().child("users").child(uid).child("multimedia").push().setValue(multimediaMsg);
        Fragment fragment = new ChatListFragment();
        cargar_fragment(fragment, "CHAT");

    }

    @Override
    public void ver_usuarios() {

        db.getReference().child("users").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                UserListFragment fragment = (UserListFragment) getSupportFragmentManager().findFragmentByTag("USERS");
                fragment.updateUserList(dataSnapshot.child("email").getValue(String.class),
                        dataSnapshot.getKey());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

    }


    @Override
    public void chatSeleccionado(int i) {

        db.getReference().child("users").child(mAuth.getUid())
                .child("multimedia").removeEventListener(cargarChatsListener);
        Fragment fragment = MessageFragment.newInstance(multimediaMsgList.get(i).getDownloadurl(),
                multimediaMsgList.get(i).getMsg(), multimediaMsgList.get(i).getName());
        cargar_fragment(fragment, "MESSAGE");
    }

    @Override
    public void cargarChats() {


        cargarChatsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                multimediaMsgList.add(dataSnapshot.getValue(MultimediaMsg.class));
                ChatListFragment fragment = (ChatListFragment) getSupportFragmentManager().findFragmentByTag("CHAT");
                fragment.addChat(dataSnapshot.child("fromEmail").getValue(String.class));

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };

        db.getReference().child("users").child(mAuth.getUid())
                .child("multimedia").addChildEventListener(cargarChatsListener);
    }

    @Override
    public void login(String email, String password) {

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("FIREBASE AUTH", "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Fragment fragment = new ChatListFragment();
                            cargar_fragment(fragment,"CHAT");
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("FIREBASE AUTH", "signInWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });



    }

    @Override
    public void signup(final String email, String password) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("FIREBASE AUTH", "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            escriure_usuari(user.getUid(), email);


                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("FIREBASE AUTH", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });

    }

    private void escriure_usuari(String uid, String email) {

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        db.getReference().child("users").child(uid).child("email").setValue(email);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Bitmap bitmap = null;

        if(requestCode == 10 && resultCode == RESULT_OK) {


            uri = data.getData();

            try {

                bitmap = MediaStore.Images.Media
                        .getBitmap(getContentResolver(), uri);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        if(bitmap != null){
            ImageChatFragment fragment = (ImageChatFragment) getSupportFragmentManager().findFragmentByTag("IMAGE");
            fragment.setFoto(bitmap);

        }


    }

    @Override
    public void descargarFoto(String url) {

        MiHilo miHilo = new MiHilo();
        miHilo.execute(url);

    }

    @Override
    public void onBackPressed() {
        try {

            if(getSupportFragmentManager().findFragmentByTag("MESSAGE") != null){
                Fragment fragment;
                fragment = (MessageFragment) getSupportFragmentManager().findFragmentByTag("MESSAGE");
                String fotoName = ((MessageFragment) fragment).getFotoName();

                fragment = new ChatListFragment();
                cargar_fragment(fragment,"CHAT");
            }

        }catch (Exception e){
            e.printStackTrace();
        }



        //super.onBackPressed();
    }

    public class MiHilo extends AsyncTask<String, Void, Bitmap> {


        @Override
        protected Bitmap doInBackground(String... strings) {

            URL url;
            HttpURLConnection connection;
            Bitmap bitmap = null;

            try {
                url = new URL(strings[0]);
                connection = (HttpURLConnection) url.openConnection();
                InputStream in = connection.getInputStream();

                bitmap = BitmapFactory.decodeStream(in);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            MessageFragment fragment = (MessageFragment) getSupportFragmentManager().findFragmentByTag("MESSAGE");
            fragment.setFoto(bitmap);

        }
    }
}
