package co.kartoo.app.cards.suggestioncard.applycard;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.kartoo.app.R;
import co.kartoo.app.ToStringConverterFactory;
import co.kartoo.app.models.ApplyCreditCardPref_;
import co.kartoo.app.models.LoginPref_;
import co.kartoo.app.receiver.SmsReceiver;
import co.kartoo.app.rest.MainService;
import co.kartoo.app.rest.model.ResponseDefault;
import co.kartoo.app.rest.model.newest.AnswerOTP;
import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

@EActivity(R.layout.activity_phone_verification)
public class PhoneVerificationActivity extends AppCompatActivity {

    @ViewById
    EditText mETotp;

    @ViewById
    Button mBtnVerify;

    @Pref
    LoginPref_ pref;

    @Pref
    ApplyCreditCardPref_ applyCreditCardPref;

    String verificationID, verificationOTP;

    ProgressDialog loadingDialog;

    SmsReceiver smsReceiver;

    @AfterViews
    void init() {


        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        smsReceiver = new SmsReceiver();

        registerReceiver(smsReceiver, filter);

        loadingDialog = new ProgressDialog(PhoneVerificationActivity.this);
        loadingDialog.setMessage("Loading...");
        loadingDialog.setIndeterminate(true);
        loadingDialog.setCancelable(false);

        Intent intent = getIntent();
        verificationID = intent.getStringExtra("verificationID");

        verificationID = verificationID.substring(1,verificationID.length()-1);

        Log.e("TAG", "verificationID: "+verificationID);

        SmsReceiver();

        //IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        //registerReceiver(smsReceiver, filter );

        new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {
                mBtnVerify.setBackground(getResources().getDrawable(R.drawable.shape_btn_coral_dead_button));
                mBtnVerify.setText(millisUntilFinished / 1000+"");
            }
            public void onFinish() {
                mBtnVerify.setBackground(getResources().getDrawable(R.drawable.shape_btn_coral_rc));
                mBtnVerify.setText("Verify");
            }
        }.start();
    }

    public void SmsReceiver(){
        SmsReceiver.bindListener(new SmsListener() {
            @Override
            public void messageReceived(String messageText) {
                Log.d("Text",messageText);
                String sms = messageText.substring(0,4);
                mETotp.setText(sms);
                verificationOTP = sms;
                callingNetwork();
                //Toast.makeText(PhoneVerificationActivity.this,"Message: "+messageText,Toast.LENGTH_LONG).show();
            }
        });
    }

    @Click(R.id.mBtnVerify)
    public void mBtnVerifyClick () {

        boolean isValid = true;
        if (mETotp.getText().toString().length() == 0) {
            mETotp.setError("OTP is required!");
            isValid = false;
            return;
        }
        if (isValid) {
            verificationOTP = mETotp.getText().toString();
            callingNetwork();
        }

    }

    public void callingNetwork(){

        loadingDialog.show();

        Log.e("TAG", "verificationOTP: "+verificationOTP );
        Log.e("TAG", "verificationID: "+verificationID );

        Retrofit retrofit = new Retrofit.Builder().baseUrl(getString(R.string.base_url)).addConverterFactory(GsonConverterFactory.create()).build();
        final MainService service = retrofit.create(MainService.class);
        AnswerOTP credentials = new AnswerOTP(verificationOTP, verificationID);

        Log.e("TAG", "isina: "+credentials.toString());

        Call<ResponseDefault> answerOTP = service.doAnswer(pref.token().get(), credentials);
        answerOTP.enqueue(new Callback<ResponseDefault>() {
            @Override
            public void onResponse(Response<ResponseDefault> response, Retrofit retrofit) {
                Log.e("TAG", "onResponse: "+response.code());
                if (response.code() == 200){
                    Intent intent = new Intent(PhoneVerificationActivity.this, ApplyProfileActivity_.class);
                    applyCreditCardPref.isVerified().put(true);
                    startActivity(intent);
                    finish();
                    loadingDialog.dismiss();
                }
                else {
                    Toast.makeText(PhoneVerificationActivity.this,"Error has occurred, please try again",Toast.LENGTH_LONG).show();
                    loadingDialog.dismiss();
                }
            }
            @Override
            public void onFailure(Throwable t) {
                Toast.makeText(PhoneVerificationActivity.this,"Error has occurred, please try again",Toast.LENGTH_LONG).show();
                loadingDialog.dismiss();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(smsReceiver);
        super.onDestroy();
    }
}
