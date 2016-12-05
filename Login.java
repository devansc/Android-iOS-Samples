package edu.calpoly.decarlso.ehabr;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleAdapter;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by devan on 4/6/16.
 */
public class Login extends FragmentActivity {
    private Button loginButton;
    private Button createAccountButton;
    private EditText email, password;
    private ArrayList<EditText> fields;
    private static EhabrApiInterface EhabrApi;
    private static final String TAG = Login.class.getSimpleName();
    public static final String PREFS_FILE = "EhabrPrefsFile";
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);
        init();
        EhabrApi = EhabrHelper.getEhabrApi();
        
        // facebook login button setup
        getFacebookHash();
        callbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = (LoginButton) findViewById(R.id.facebook_login_button);
        loginButton.setReadPermissions(Arrays.asList("email", "user_birthday", "user_hometown"));
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(LoginResult loginResult) {

                System.out.println("onSuccess");
                String accessToken = loginResult.getAccessToken().getToken();
                Log.i("accessToken", accessToken);

                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {

                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        Log.i("LoginActivity", response.toString());
                        facebookLogin(object);

                    }
                });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "email, name, hometown");
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {
                System.out.println("onCancel");
            }

            @Override
            public void onError(FacebookException exception) {
                System.out.println("onError");
                Log.v("LoginActivity", exception.getCause().toString());
            }
        });
        LoginManager.getInstance().logOut();
        AppEventsLogger.activateApp(this);
    }

    public class FacebookLogin extends User {
        public String FacebookID;
        public FacebookLogin(String FacebookID, String Name, String Email, String Password) {
            super(Name, Email, Password);
            this.FacebookID = FacebookID;
        }
    }

    private void facebookLogin(JSONObject object) {

        String email = "";
        String name = "";
        String fbookID = "facebookID";
        try {
            email = object.getString("email");
            name = object.getString("name");
            fbookID = object.getString("id");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        FacebookLogin login = new FacebookLogin(fbookID, name, email, "");
        Call<Account> call = EhabrApi.facebookLogin(login);

        call.enqueue(new Callback<Account>() {
                         @Override
                         public void onResponse(Call<Account> call, Response<Account> response) {
                             int statusCode = response.code();
                             System.out.flush();
                             Log.d(TAG, "Recieved " + statusCode);
                             if (response.isSuccessful()) {
                                 SharedPreferences prefs = getSharedPreferences(PREFS_FILE, 0);
                                 SharedPreferences.Editor editor = prefs.edit();
                                 editor.putString("AccountID", response.body().getAccountID());
                                 editor.putString("Name", response.body().getName());
                                 System.out.println("put account id "+ response.body().getAccountID());
                                 editor.commit();
                                 startActivity(new Intent(Login.this, FindRestaurants.class));
                             } else {
                                 Common.showPopup(Login.this, "Error", "Invalid email or password");
                             }
                         }

                         @Override
                         public void onFailure(Call<Account> call, Throwable t) {
                             Log.d(TAG, "Failed call " + t);
                             Log.d(TAG, "stack trace: ");
                             t.printStackTrace();
                         }
                     }
        );
    }


    // for facebook
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void getFacebookHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "edu.calpoly.decarlso.ehabr",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("Facebook KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void init() {

        SharedPreferences prefs = getSharedPreferences(Login.PREFS_FILE, 0);
        String accountID = prefs.getString("AccountID", "");
        System.out.println("Found account id " + accountID);
        if (!accountID.equals("")) {
            startActivity(new Intent(Login.this, FindRestaurants.class));
            return;
        }

        fields = new ArrayList<EditText>();
        loginButton = (Button) findViewById(R.id.loginButton);
        createAccountButton = (Button) findViewById(R.id.createAccountButton);
        email = (EditText) findViewById(R.id.Email);
        password = (EditText) findViewById(R.id.Password);
        fields.add(email);
        fields.add(password);
        //createAccountButton.setOnClickListener(this);
        password.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    loginButton.performClick();
                }
                return false;
            }
        });
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });
        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go to create account activity
                Intent createAccount = new Intent(Login.this, CreateAccount.class);
                SharedPreferences prefs = getSharedPreferences(PREFS_FILE, 0);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("Email", email.getText().toString());
                editor.putString("Password", password.getText().toString());
                editor.commit();

                System.out.println("put email and password in extra " + email.getText());
                startActivity(createAccount);
            }
        });


        email.setText(prefs.getString("Email", ""));
        password.setText(prefs.getString("Password", ""));


    }




    private void login() {
        System.out.println("In login");
        LoginCreds lc = new LoginCreds(email.getText().toString(), password.getText().toString());

        if (Common.checkAllFields(this, fields)) { // missing a field
            return;
        }

        Call<Account> call = EhabrApi.login(lc);

        call.enqueue(new Callback<Account>() {
                         @Override
                         public void onResponse(Call<Account> call, Response<Account> response) {
                             int statusCode = response.code();
                             System.out.flush();
                             Log.d(TAG, "Received " + statusCode);
                             if (response.isSuccessful()) { // make sure we get a 200 or else invalid credentials 
                                 SharedPreferences prefs = getSharedPreferences(PREFS_FILE, 0);
                                 SharedPreferences.Editor editor = prefs.edit();
                                 editor.putString("AccountID", response.body().getAccountID());
                                 editor.putString("Name", response.body().getName());
                                 editor.commit();
                                 startActivity(new Intent(Login.this, FindRestaurants.class));
                             } else {
                                 Common.showPopup(Login.this, "Error", "Invalid email or password");
                             }
                         }

                         @Override
                         public void onFailure(Call<Account> call, Throwable t) {
                             Log.d(TAG, "Failed call " + t);
                             Log.d(TAG, "stack trace: ");
                             t.printStackTrace();
                         }
                     }
        );

    }
}
