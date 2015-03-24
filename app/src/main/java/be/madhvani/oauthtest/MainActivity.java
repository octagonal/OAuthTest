package be.madhvani.oauthtest;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.sax.TextElementListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.*;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new TwitterLogin())
                    .commit();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

     public static class TwitterLogin extends Fragment {

         public static final String SETTINGS_FILENAME = "Settings";
         public static final String TWEET = "https://api.twitter.com/1.1/statuses/update.json";
         private static final String PROTECTED_RESOURCE_URL = "https://api.twitter.com/1.1/account/verify_credentials.json";

         public TwitterLogin() {
        }

        private TextView authUrl;
        private EditText pin;

        private Button button;
        private Button accessToken;

        private OAuthService service;
        private Token requestToken;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            service = new ServiceBuilder()
                    .provider(TwitterApi.class)
                    .apiKey(getResources().getString(R.string.apiKey))
                    .apiSecret(getResources().getString(R.string.apiSecret))
                    .build();

            authUrl = (TextView) rootView.findViewById(R.id.authUrl);
            pin = (EditText) rootView.findViewById(R.id.pin);

            accessToken = (Button) rootView.findViewById(R.id.accessToken);
            accessToken.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AsyncTask authTask = new TwitterGetTokenTask(TwitterLogin.this).execute(pin.getText().toString());
                    Toast.makeText(getActivity(), "", Toast.LENGTH_LONG).show();
                }
            });

            button = (Button) rootView.findViewById(R.id.OAuthButton);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AsyncTask authTask = new TwitterAuthUrlTask(service, TwitterLogin.this).execute();
                    Toast.makeText(getActivity(),"",Toast.LENGTH_LONG).show();
                }
            });
            return rootView;
        }

        private class TwitterGetTokenTask extends AsyncTask<String, Void, String> {

            private TwitterLogin placeholderFragment;

            public TwitterGetTokenTask(TwitterLogin placeholderFragment){
                this.placeholderFragment = placeholderFragment;
            }

            @Override
            public void onPostExecute(String url){
                SharedPreferences settings = getActivity().getSharedPreferences("Settings", 0);
                String accessToken = settings.getString("accessToken", "missing");
                String tokenSecret = settings.getString("tokenSecret", "missing" );

                Log.i("TwitterGetTokenTask", accessToken);
                Log.i("TwitterGetTokenTask", tokenSecret);

                placeholderFragment.authUrl.setText(accessToken + "\n" + tokenSecret);
            }

            @Override
            public String doInBackground(String... pins){
                SharedPreferences settings = getActivity().getSharedPreferences(SETTINGS_FILENAME, 0);
                String access = settings.getString("accessToken", null);
                String secret = settings.getString("tokenSecret", null);

                Token accessToken;
                if(access != null && secret != null){
                    accessToken = new Token(access, secret);
                } else {
                    Verifier verifier = new Verifier(pins[0]);
                    accessToken = service.getAccessToken(TwitterLogin.this.requestToken, verifier);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("accessToken", accessToken.getToken());
                    editor.putString("tokenSecret", accessToken.getSecret());
                    editor.commit();
                }

                OAuthRequest request = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL);
                //OAuthRequest request = new OAuthRequest(Verb.POST, TWEET);
                request.addBodyParameter("status", "Posting this from my emulator");

                service.signRequest(accessToken, request);
                Response response = request.send();
                System.out.println(response.getBody());
                JSONObject resp = new JSONObject();
                try {
                    resp = new JSONObject(response.getBody());
                } catch(JSONException e){
                    Log.e("TwitterGetTokenTask", "Json Exception", e);
                }
                return resp.toString();
            }
        }

        private class TwitterAuthUrlTask extends AsyncTask<Void, Void, String> {

            private TwitterLogin placeholderFragment;
            private OAuthService service;

            public TwitterAuthUrlTask(OAuthService service, TwitterLogin placeholderFragment){
                this.placeholderFragment = placeholderFragment;
                this.service = service;
            }

            @Override
            public void onPostExecute(String url){
                placeholderFragment.authUrl.setText(url);
            }

            @Override
            public String doInBackground(Void... params){
                TwitterLogin.this.requestToken = service.getRequestToken();
                System.out.println(service.getAuthorizationUrl(TwitterLogin.this.requestToken));
                return service.getAuthorizationUrl(TwitterLogin.this.requestToken);
            }
        }
    }
}

