package dream.africa.activities;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.v7.app.AppCompatActivity;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.Intent;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import dream.africa.R;
import dream.africa.base.utils.PrefUtils;

public class FacebookLogin extends AppCompatActivity {

    public static String LOGGED_IN = "logged_in";
    public static String FB_ID = "facebook_id";
    public static String FB_TOKEN = "facebook_token";

    CallbackManager callbackManager;
    private TextView info;
    private LoginButton loginButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "air.com.yudu.ReaderAIR3663672",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }

        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this.getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        setContentView(R.layout.activity_facebook_login);
        loginButton = (LoginButton)findViewById(R.id.login_button);
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                startActivity(new Intent(FacebookLogin.this, MainActivity.class));
                PrefUtils.save(FacebookLogin.this, LOGGED_IN, true);
//              PrefUtils.save(FacebookLogin.this, FB_ID, (String) loginResult.getAccessToken().getUserId());
//              PrefUtils.save(FacebookLogin.this, FB_TOKEN, (String) loginResult.getAccessToken().getToken());
            }

            @Override
            public void onCancel() {
                //info.setText("Login attempt canceled.");
            }

            @Override
            public void onError(FacebookException e) {
                //info.setText("Login attempt failed.");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }


}
