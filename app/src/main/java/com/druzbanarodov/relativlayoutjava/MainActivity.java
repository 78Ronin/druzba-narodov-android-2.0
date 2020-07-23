package com.druzbanarodov.relativlayoutjava;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

//import android.support.v7.app.AppCompatActivity;

//import android.content.DialogInterface;

public class MainActivity extends AppCompatActivity
{

    private FirebaseAuth mAuth;

    private static final String TAG = "EmailPassword";

    //variables
    Button show, show2, getStarted, Continue;
    EditText edit_password, edit_name, edit_email, edit_password2;
    TextView toast, name_display, forget;
    private final String Default = "N/A";
    String[] Gender = {"Муж", "Жен"};
    String gender;
    final static String TARGET_BASE_PATH = "/sdcard/appname/voices/";
    Spinner spinner;
    ImageView icon_user;
    private ProgressDialog progressBar;//Create a circular progressBar Dialog
    //private android.content.Context ActivityContext;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        final SharedPreferences sharedPreferences = getSharedPreferences("Content_main", Context.MODE_PRIVATE);
        String name_file = sharedPreferences.getString("name", Default);
        String pass_file = sharedPreferences.getString("password", Default);
        String email_file = sharedPreferences.getString("email", Default);
        String gender_file = sharedPreferences.getString("gender", Default);

        progressBar = new ProgressDialog(this);
        progressBar.setCancelable(false);
        progressBar.setMessage("Проверка профиля...");
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.setProgress(0);
        progressBar.setMax(100);

        if (name_file.equals(Default) || pass_file.equals(Default) || email_file.equals(Default) || gender_file.equals(Default))
        {
            // Пользователь не существует в телефоне

            setContentView(R.layout.activity_main);

            show = (Button) findViewById(R.id.show);
            edit_password = (EditText) findViewById(R.id.password);
            edit_email = (EditText) findViewById(R.id.email);
            edit_name = (EditText) findViewById(R.id.name);
            show.setOnClickListener(new showOrHidePassword());
            toast = (TextView) findViewById(R.id.toast_help);
            spinner = (Spinner) findViewById(R.id.spinner);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner, Gender);
            adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new spinner());
            getStarted = (Button) findViewById(R.id.getStarted);

            getStarted.setOnClickListener(v ->
            {
                String save_name = edit_name.getText().toString();
                //String save_name = "D1";
                String save_email = edit_email.getText().toString();
                //String save_email = "d1@rambler.ru";
                String save_password = edit_password.getText().toString();
                //String save_password = "19Mama#";

                if (save_name.equals("") || save_email.equals("") || save_password.equals(""))
                {
                    try
                    {
                        Toast.makeText(MainActivity.this, "Пожалуйста, укажите детали", Toast.LENGTH_SHORT).show();
                    }
                    catch (Exception ex)
                    { }
                }
                else
                {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("name", save_name);
                    editor.putString("password", save_password);
                    editor.putString("email", save_email);
                    editor.putString("gender", gender);

                    // Приложение было удалено и поставлено вновь. Попытка регистрации под тем же e-mail
                    asyncSignInOrCreateUser(save_email, save_password, editor);
                }
            });
        }
        else
        {
            // Пользователь есть в телефоне
            asyncSignInOrCreateUser(email_file, pass_file, null);
        }
    }

    private void asyncSignInOrCreateUser(final String email, final String password, final SharedPreferences.Editor editor)
    {
        progressBar.show();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(MainActivity.this, taskResult1 ->
                {
                    try
                    {
                        taskResult1.getResult();

                        // Аккаунт уже существует на сервере
                        updateUI(mAuth.getCurrentUser());
                        Intent intent = new Intent(MainActivity.this, Navigation_Nome_Menu.class);
                        startActivity(intent);
                        MainActivity.this.finish();
                        progressBar.cancel();
                        Toast.makeText(MainActivity.this, "Успешная авторизация", Toast.LENGTH_SHORT).show();
                        if (editor != null) editor.commit();
                    }
                    catch (Exception ex1)
                    {
                        switch (ex1.getMessage())
                        {
                            case "com.google.firebase.auth.FirebaseAuthInvalidUserException: There is no user record corresponding to this identifier. The user may have been deleted.":
                                // Регистрация на сервере
                                mAuth.createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener(MainActivity.this, taskResult2 ->
                                        {
                                            try
                                            {
                                                updateUI(mAuth.getCurrentUser());
                                                Intent intent = new Intent(MainActivity.this, Navigation_Nome_Menu.class);
                                                startActivity(intent);
                                                MainActivity.this.finish();
                                                progressBar.cancel();
                                                Toast.makeText(MainActivity.this, "Успешная регистрация", Toast.LENGTH_SHORT).show();
                                                if (editor != null) editor.commit();
                                            }
                                            catch (Exception ex2)
                                            {
                                                Toast.makeText(MainActivity.this, "Проблемы с сетью!", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                break;
                            case "com.google.firebase.FirebaseNetworkException: A network error (such as timeout, interrupted connection or unreachable host) has occurred.":
                                Toast.makeText(MainActivity.this, "Проблемы с сетью!", Toast.LENGTH_SHORT).show();
                                progressBar.cancel();
                                break;
                        }
                    }
                });
    }

    protected void onCreate2(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences sharedPreferences = getSharedPreferences("Content_main", Context.MODE_PRIVATE);//reference to shared preference file

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        //Creating a shared preference file  to save the name ,mail address,password and also for setting the correct xml file
        String name_file = sharedPreferences.getString("name", Default);
        String pass_file = sharedPreferences.getString("password", Default);
        String email_file = sharedPreferences.getString("email", Default);
        String gender_file = sharedPreferences.getString("gender", Default);
        SharedPreferences sp = getSharedPreferences("Score", Context.MODE_PRIVATE);
        if (name_file.equals(Default) || pass_file.equals(Default) || email_file.equals(Default) || gender_file.equals(Default)) {

            setContentView(R.layout.activity_main);
            NavigationView navigationView = findViewById(R.id.nav_view);
//            navigationView.setItemIconTintList(null);

            show = (Button) findViewById(R.id.show);  //Show button in password
            edit_password = (EditText) findViewById(R.id.password);   //Password EditText
            edit_email = (EditText) findViewById(R.id.email);   //email EditText
            edit_name = (EditText) findViewById(R.id.name);   //name EditText
            show.setOnClickListener(new showOrHidePassword());//invoking the showOrHidePassword class to show the password
            toast = (TextView) findViewById(R.id.toast_help);//toast_help object


            //Spinner for choosing the gender
            spinner = (Spinner) findViewById(R.id.spinner);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner, Gender);
            adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new spinner());
            //
            //Get started registration
            getStarted = (Button) findViewById(R.id.getStarted);
            getStarted.setOnClickListener(new View.OnClickListener()
            {
                    @Override
                    public void onClick(View v) {
                        //String save_name = edit_name.getText().toString();
                        String save_name = "Dmitry";
                        //System.out.println(""+save_name);
                        //String save_email = edit_email.getText().toString();
                        String save_email = "tcherbakoff2@rambler.ru";
                        //System.out.println(""+save_email);
                        //String save_password = edit_password.getText().toString();
                        String save_password = "19Mama#";

                    //If and else are used to check if all the three text field are empty or not
                    if (save_name.equals("") || save_email.equals("") || save_password.equals("")) {
                        try{
                            Toast.makeText(MainActivity.this, "Пожалуйста, укажите детали", Toast.LENGTH_SHORT).show();
                        }
                        catch (Exception e)
                        {}
                    }
                    else {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("name", save_name);
                        editor.putString("password", save_password);
                        editor.putString("email", save_email);
                        editor.putString("gender", gender);
                        editor.commit();

                        //write data to Firebase
                        mAuth.createUserWithEmailAndPassword(save_email, save_password)
                                .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            // Sign in success, update UI with the signed-in user's information
                                            Log.d(TAG, "createUserWithEmail:success");
                                            FirebaseUser user = mAuth.getCurrentUser();
                                            updateUI(user);
                                        } else {
                                            // If sign in fails, display a message to the user.
                                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                                    Toast.LENGTH_SHORT).show();
                                            updateUI(null);
                                        }
                                    }
                                });

                        progressBar = new ProgressDialog(v.getContext());//Create new object of progress bar type
                        progressBar.setCancelable(false);//Progress bar cannot be cancelled by pressing any where on screen
                        progressBar.setMessage("Создание профиля...");//Title shown in the progress bar
                        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);//Style of the progress bar
                        progressBar.setProgress(0);//attributes
                        progressBar.setMax(100);//attributes
                        progressBar.show();//show the progress bar
                        //This handler will add a delay of 3 seconds
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //Intent start to open the navigation drawer activity
                                progressBar.cancel();//Progress bar will be cancelled (hide from screen) when this run function will execute after 3.5seconds
                                Intent intent = new Intent(MainActivity.this, Navigation_Nome_Menu.class);
                                startActivity(intent);
                                finish();
                            }
                        }, 3500);

                    }

                }
            });

        //if user was created
        }
        else
        {
            setContentView(R.layout.activity_main_second);
            icon_user = (ImageView) findViewById(R.id.image_icon);
            if (gender_file.equals("Муж")) {
                icon_user.setImageResource(R.drawable.man);
            } else {
                icon_user.setImageResource(R.drawable.female);
            }


            name_display = (TextView) findViewById(R.id.name_display);
            name_display.setText(name_file);
            edit_password2 = (EditText) findViewById(R.id.password2);
            show2 = (Button) findViewById(R.id.show2);
            show2.setOnClickListener(new showOrHidePassword2());
            forget = (TextView) findViewById(R.id.forget);
            Continue = (Button) findViewById(R.id.Continue);

            try {
                Continue.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        String local_pass2 = edit_password2.getText().toString();
                        if (sharedPreferences.getString("password", Default).equals(local_pass2)) {

                            mAuth.signInWithEmailAndPassword(sharedPreferences.getString("email", Default), sharedPreferences.getString("password", Default))
                                    .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful()) {
                                                // Sign in success, update UI with the signed-in user's information
                                                Log.d(TAG, "signInWithEmail:success");
                                                FirebaseUser user = mAuth.getCurrentUser();
                                                updateUI(user);
                                            } else {
                                                // If sign in fails, display a message to the user.
                                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                                Toast.makeText(MainActivity.this, "Authentication failed.",
                                                        Toast.LENGTH_SHORT).show();
                                                updateUI(null);
                                            }
                                        }
                                    });

                            progressBar = new ProgressDialog(v.getContext());//Create new object of progress bar type
                            progressBar.setCancelable(false);//Progress bar cannot be cancelled by pressing any wher on screen
                            progressBar.setMessage("Пожалуйста, подождите...");//Tiitle shown in the progress bar
                            progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);//Style of the progress bar
                            progressBar.setProgress(0);//attributes
                            progressBar.setMax(100);//attributes
                            progressBar.show();//show the progress bar
                            //This handler will add a delay of 3 seconds
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //Intent start to open the navigation drawer activity
                                    progressBar.cancel();//Progress bar will be cancelled (hide from screen) when this run function will execute after 3.5seconds
                                    Intent intent = new Intent(MainActivity.this, Navigation_Nome_Menu.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }, 2500);

                        } else {
                            Toast.makeText(MainActivity.this, "Пожалуйста, введите верный пароль", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Предупреждение", Toast.LENGTH_SHORT).show();
            }

        }



    }



    //Used to show the help by triggering a toast
    public void showHelp(View view) {

        Toast toast_help = new Toast(getApplicationContext());
        toast_help.setGravity(Gravity.CENTER, 0, 0);
        toast_help.setDuration(Toast.LENGTH_LONG);
        LayoutInflater inflater = getLayoutInflater();
        View appear = inflater.inflate(R.layout.toast_help, (ViewGroup) findViewById(R.id.linear));
        toast_help.setView(appear);
        toast_help.show();

    }


    //Used to add some time so that user cannot directly press and exity out of the activity
    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 4000);

    }


    //class to show or hide password on button click in main activity
    class showOrHidePassword implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (show.getText().toString() == "Показать") {
                edit_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                show.setText("Скрыть");

            } else {

                edit_password.setTransformationMethod(PasswordTransformationMethod.getInstance());
                show.setText("Показать");
            }
        }
    }

    //class to show or hide password on button click in main activity second
    class showOrHidePassword2 implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (show2.getText().toString() == "Показать") {
                edit_password2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                show2.setText("Скрыть");

            } else {

                edit_password2.setTransformationMethod(PasswordTransformationMethod.getInstance());
                show2.setText("Показать");
            }
        }
    }


    //Spinner class to select spinner and also add gender
    class spinner implements AdapterView.OnItemSelectedListener {


        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            gender = parent.getItemAtPosition(position).toString();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //When nothing is selected
            Toast.makeText(getApplicationContext(), "Please Enter the gender", Toast.LENGTH_SHORT).show();
        }
    }

    public void showDialog(View view) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_pressed}, // pressed
                new int[]{android.R.attr.state_enabled}
        };

        int[] colors = new int[]{
                Color.parseColor("#9B1D20"), // red
                Color.parseColor("#AAFAC8") //light green

        };

        ColorStateList list = new ColorStateList(states, colors);
        forget.setTextColor(list);

        AlertDialog.Builder alertDialog;//Create a dialog object
        alertDialog = new AlertDialog.Builder(MainActivity.this);
        //EditText to show up in the AlertDialog so that the user can enter the email address
        final EditText editTextDialog = new EditText(MainActivity.this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        editTextDialog.setLayoutParams(layoutParams);
        editTextDialog.setHint("Email");
        //Adding EditText to Dialog Box
        alertDialog.setView(editTextDialog);
        alertDialog.setTitle("Введите Email");
        final SharedPreferences sharedPreferences = getSharedPreferences("Content_main", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        alertDialog.setPositiveButton("Отправить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email_dialog = editTextDialog.getText().toString();
                if (sharedPreferences.getString("email", Default).equals(email_dialog)) {
                    //We are setting the values of Prefrences in sharedPrefrences to Default
                    editor.putString("name", Default);
                    editor.putString("email", Default);
                    editor.putString("password", Default);
                    editor.putString("gender", Default);
                    editor.commit();

                    //This intent will call the package manager and restart the current activity
                    Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                } else {
                    Toast.makeText(MainActivity.this, "Введите действительный адрес электронной почты", Toast.LENGTH_SHORT).show();
                }
            }

        });
        alertDialog.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //When the Disagree button is pressed
            }
        });
        //Showing up the alert dialog box
        alertDialog.show();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void updateUI(FirebaseUser currentUser) {


    }


}
