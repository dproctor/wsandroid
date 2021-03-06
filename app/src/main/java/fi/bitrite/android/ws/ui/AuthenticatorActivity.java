package fi.bitrite.android.ws.ui;

import android.accounts.Account;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.auth.AccountManager;
import fi.bitrite.android.ws.auth.AuthToken;
import fi.bitrite.android.ws.di.Injectable;
import fi.bitrite.android.ws.ui.util.ProgressDialog;
import fi.bitrite.android.ws.ui.view.AccountAuthenticatorFragmentActivity;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

/**
 * The activity responsible for getting Warmshowers credentials from the user,
 * verifying them against the Warmshowers web service and storing
 * them on the device using Android's custom account facilities.
 */
public class AuthenticatorActivity extends AccountAuthenticatorFragmentActivity
        implements Injectable {

    @Inject AccountManager mAuthenticationManager;

    @BindView(R.id.auth_txt_username) EditText mTxtUsername;
    @BindView(R.id.auth_txt_password) EditText mTxtPassword;
    @BindView(R.id.auth_btn_login) Button mBtnLogin;

    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            setLoginButtonEnabled(areAllInputsNotEmpty());
        }
    };

    private Disposable mProgressDisposable;

    private Disposable mResumePauseDisposable;
    private BehaviorSubject<LoginResult> mLoginResult = BehaviorSubject.create();

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_authentication);
        ButterKnife.bind(this);

        // Deactivates login button to prevent requests from being made with incomplete login data
        setLoginButtonEnabled(areAllInputsNotEmpty());
        mTxtUsername.addTextChangedListener(mTextWatcher);
        mTxtPassword.addTextChangedListener(mTextWatcher);

        // If we do a re-login (to obtain a new auth token), the username is provided by the caller.
        String providedUsername =
                getIntent().getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME);
        if (!TextUtils.isEmpty(providedUsername)) {
            mTxtUsername.setText(providedUsername);
            disableEditText(mTxtUsername);

            // The username is set -> we set the focus on the password field.
            mTxtPassword.requestFocus();
        }

        // Shows the keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // We only listen to login results as long as the app is in the foreground. If it was in the
        // background when the result appeared we catch up on taking care of it as the very next
        // thing.
        mResumePauseDisposable = mLoginResult
                .filter(result -> !result.isHandled)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    result.isHandled = true;

                    if (result.throwable != null) {
                        mProgressDisposable.dispose();
                        Toast.makeText(this, R.string.http_server_access_failure, Toast.LENGTH_LONG)
                                .show();
                    } else if (result.response != null) {
                        // The following we add for usage in MainActivity::onActivityResult().
                        Intent intent = new Intent();
                        intent.putExtras(result.response);
                        setResult(0, intent);
                        setAccountAuthenticatorResult(result.response);
                        finish();
                    } else {
                        mProgressDisposable.dispose();
                        Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG)
                                .show();
                    }
                });
    }

    @Override
    protected void onPause() {
        mResumePauseDisposable.dispose();
        super.onPause();
    }

    /**
     * Changes the enabled state of the login button
     *
     * @param enabled If true, login button is set to activated, if false it's deactivated
     */
    @VisibleForTesting
    void setLoginButtonEnabled(boolean enabled) {
        mBtnLogin.setEnabled(enabled);
    }

    /**
     * @return True if both username and password input are empty, false otherwise.
     */
    @VisibleForTesting
    boolean areAllInputsNotEmpty() {
        return !TextUtils.isEmpty(mTxtUsername.getText())
               && !TextUtils.isEmpty(mTxtPassword.getText());
    }

    /**
     * Completely disables specified edit text, rendering it like a TextView.
     *
     * @param editText EditText which should be disabled
     */
    private void disableEditText(EditText editText) {
        editText.setBackgroundColor(Color.TRANSPARENT);
        editText.setCursorVisible(false);
        editText.setEnabled(false);
        editText.setFocusable(false);
        editText.setKeyListener(null);
    }


    @OnClick(R.id.auth_btn_login)
    public void login() {
        String username = mTxtUsername.getText().toString();
        String password = mTxtPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            return;
        }

        mProgressDisposable = ProgressDialog.create(R.string.authenticating).show(this);

        // The following callback can be happening when the app is pushed to the background. We
        // update the {@link mLoginResult} observable, s.t. we only react on the change when the app
        // is in the foreground.
        Disposable unused = mAuthenticationManager.login(username, password)
                .subscribe(result -> {
                    Bundle response = new Bundle();
                    response.putString(android.accounts.AccountManager.KEY_ACCOUNT_NAME, username);

                    String accountType = getIntent().getStringExtra(
                            android.accounts.AccountManager.KEY_ACCOUNT_TYPE);
                    response.putString(
                            android.accounts.AccountManager.KEY_ACCOUNT_TYPE, accountType);

                    boolean isAlreadyLoggedIn = 406 == result.response().code();
                    if (result.isSuccessful() || isAlreadyLoggedIn) {
                        AuthToken authToken;
                        if (isAlreadyLoggedIn) {
                            // 406 Not Acceptable : Already logged in as [xxx]. ()
                            // This error can occur if an additional account is added, which in fact
                            // is already logged in. We just mark the login as successful and
                            // continue.
                            Account account = new Account(username, accountType);
                            authToken = mAuthenticationManager.peekAuthToken(account);
                        } else {
                            // 200 OK - The login was successful.
                            authToken = result.authData().authToken;
                        }

                        // Required if this activity was shown in getAuthToken but the authToken was
                        // previously invalidated.
                        String authTokenStr = authToken == null ? null : authToken.toString();
                        response.putString(
                                android.accounts.AccountManager.KEY_AUTHTOKEN, authTokenStr);

                        mLoginResult.onNext(new LoginResult(response));
                    } else {
                        mLoginResult.onNext(new LoginResult());
                    }
                }, error -> mLoginResult.onNext(new LoginResult(error)));
    }

    /**
     * Starts the user's browser and opens the password reset page
     * for Warmshowers if forgot password button was clicked
     */
    @OnClick(R.id.auth_btn_forgot_password)
    public void forgotPassword() {
        final String passwordResetPageUrl = getString(R.string.url_target_forgot_password);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(passwordResetPageUrl)));
    }

    @Override
    public void onBackPressed() {
        // Disallow it.
    }

    private class LoginResult {
        final Bundle response;
        final Throwable throwable;
        boolean isHandled = false;

        private LoginResult() {
            response = null;
            throwable = null;
        }
        private LoginResult(@NonNull Bundle response) {
            this.response = response;
            this.throwable = null;
        }
        private LoginResult(@NonNull Throwable throwable) {
            this.response = null;
            this.throwable = throwable;
        }
    }
}
