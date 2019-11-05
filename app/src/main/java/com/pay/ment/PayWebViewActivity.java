package com.pay.ment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.unionpay.UPPayAssistEx;
import com.unionpay.mobile.android.plugin.BaseActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class PayWebViewActivity extends AppCompatActivity implements Handler.Callback,
        Runnable {

    ImageView payBack;
    RelativeLayout rlTitle;
    View viewJustview;
    ImageView ivCircleTop;
    TextView tvOtherLeft;
    TextView tvOtherRight;
    LinearLayout llOtherContent;
    ProgressBar progressBar;
    ImageView ivClose;
    FrameLayout flContent;
    WebView webview;

    public static final int PAY_PUSH = 10010;
    public static final int PAY_CODE = 1024;

    String mode = "";
    String otherRight;
    //    String url = "http://fina.housecenter.cn//cashier/app/internal?data=%7B%22clientip%22%3A%2210.39.8.154%22%2C%22orderid%22%3A%220710015583458305177230%22%2C%22posid%22%3A%2200006%22%2C%22regInfo%22%3A%22bec7cf5471ec43bdaf6c40951116e7e8%22%7D&serialVersionUID=1&sign=0E2083DF6DF498AFD7D471D0568ADEE3";
    String url = "";

    boolean isTitle = false;

    @SuppressLint("HandlerLeak")
    Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    viewJustview.setVisibility(View.GONE);
                    llOtherContent.setVisibility(View.GONE);
                    ivCircleTop.setVisibility(View.GONE);
                    ivClose.setVisibility(View.GONE);
                    break;

            }
        }
    };

    public static final String LOG_TAG = "PayDemo";
    private Context mContext = null;
    private Handler mHandler = null;

    /*****************************************************************
     * mMode参数解释： "00" - 启动银联正式环境 "01" - 连接银联测试环境
     *****************************************************************/
    private String mMode = "00";
    //    private static final String TN_URL_01 = "http://101.231.204.84:8091/sim/getacptn";
//    private static final String TN_URL = "http://10.32.158.15/cashier/app/notifypage";
    private static final String TN_URL = "http://fina.housecenter.cn/cashier/app/notifypage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paywebview);
        mContext = this;
        mHandler = new Handler(this);
        initWebView();
    }

    private void initWebView() {
        payBack = (ImageView) findViewById(R.id.payBack);
        rlTitle = (RelativeLayout) findViewById(R.id.rl_title);
        viewJustview = (View) findViewById(R.id.view_justview);
        ivCircleTop = (ImageView) findViewById(R.id.iv_circle_top);
        tvOtherLeft = (TextView) findViewById(R.id.tv_other_left);
        tvOtherRight = (TextView) findViewById(R.id.tv_other_right);
        llOtherContent = (LinearLayout) findViewById(R.id.ll_other_content);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        ivClose = (ImageView) findViewById(R.id.iv_close);
        flContent = (FrameLayout) findViewById(R.id.fl_content);
        webview = (WebView) findViewById(R.id.webView);

        Intent intent = getIntent();
        url = intent.getStringExtra("URL");
        llOtherContent.setVisibility(View.VISIBLE);
        mode = intent.getStringExtra("MODE");
        otherRight = intent.getStringExtra("ORDER_NO");
        tvOtherRight.setText(otherRight);
        if (mode.equals("0")) {
            mMode = "00";
        } else if (mode.equals("1")) {
            mMode = "01";
        }

        ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                outAnimation();
            }
        });

        payBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        WebSettings webSettings = webview.getSettings();
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                /*view.loadUrl(url);
                return true;*/
                view.loadUrl(url);
                return true;
            }
        });
        String appCachePath = PayWebViewActivity.this.getCacheDir().getAbsolutePath();
        webview.getSettings().setAppCachePath(appCachePath);
        webview.getSettings().setAllowFileAccess(true);
        webview.getSettings().setAppCacheEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setJavaScriptEnabled(true);
        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (isTitle) {
                    rlTitle.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                // TODO Auto-generated method stub
                super.onProgressChanged(view, newProgress);
                if (progressBar == null) {
                    return;
                }
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                    isTitle = true;
                } else {
                    if (progressBar.getVisibility() == View.GONE)
                        progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                }
            }
        });
        webview.clearCache(true);
        if (TextUtils.isEmpty(url) /*|| !RegularUtils.isURL(url)*/) {
            url = "about:blank";
        }
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept-Language", getLang(PayWebViewActivity.this));
        headers.put("flag", getIntent().getStringExtra("C_TOKEN"));
        webview.loadUrl(url, headers);
        webview.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webview.addJavascriptInterface(this, "android");
//        new Thread(PayWebViewActivity.this).start();
//        Map<String, String> map = new HashMap<>();
//        map.put("sign", "R5UuNEJ6X5xlPIqzbahANJQ81WST8JBBThKzZF6/ZI/YWE4EN3U6BcwAk9EBPVdvh9Tl//HVggxtQUa20KoXzKZm/vJm7f7DkAwtObXAuqu5kh3T3Gyvrc7veRZHX2YQ62HYkmWrtXt2tUqNt3Bd03f9ShrDdVoStmex4e3PELuHE/ejWd4sHusxJSvTOHjIeYksB4s14NskEqHVRzHHnKK1muq0+DneQFkqbE7XsYiyKdyKHvk7VKM7f3x0I/385C9x8zxdGJvyzWIpQ1xNy2VbFNAWv+bcEnByr5s5K+hDHJu+Qbb4qRqiUuu8lfaiVxynS/K/osxxRsL+yYrn4Q==");
//        map.put("data", "pay_result=success&tn=633189658221127582801&cert_id=69026276696");
//        map.put("SOURCE", "6");
//        postRequset(TN_URL, map);
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.e(LOG_TAG, " " + "" + msg.obj);

        String tn = "";
        if (msg.obj == null || ((String) msg.obj).length() == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("错误提示");
            builder.setMessage("网络连接失败,请重试!");
            builder.setNegativeButton("确定",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.create().show();
        } else {
            tn = (String) msg.obj;
            Toast.makeText(mContext, tn, Toast.LENGTH_SHORT).show();
            /*************************************************
             * 步骤2：通过银联工具类启动支付插件
             ************************************************/
//            doStartUnionPayPlugin(this, "627329473805345710900", mMode);
//            doStartUnionPayPlugin(this, tn, mMode);
//            UPPayAssistEx.startPay(PayWebViewActivity.this, null, null, tn, mMode);
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*************************************************
         * 步骤3：处理银联手机支付控件返回的支付结果
         ************************************************/
        if (data == null) {
            return;
        }

        String msg = "";
        /*
         * 支付控件返回字符串:success、fail、cancel 分别代表支付成功，支付失败，支付取消
         */
        String str = data.getExtras().getString("pay_result");
        String result = "";
        String sign = "";
        String dataOrg = "";
        if (str.equalsIgnoreCase("success")) {

            // 如果想对结果数据验签，可使用下面这段代码，但建议不验签，直接去商户后台查询交易结果
            // result_data结构见c）result_data参数说明
            if (data.hasExtra("result_data")) {
                result = data.getExtras().getString("result_data");
                try {
                    JSONObject resultJson = new JSONObject(result);
                    sign = resultJson.getString("sign");
                    dataOrg = resultJson.getString("data");
                    // 此处的verify建议送去商户后台做验签
                    // 如要放在手机端验，则代码必须支持更新证书
                    boolean ret = verify(dataOrg, sign, mMode);
                    if (ret) {
                        // 验签成功，显示支付结果
                        msg = "支付成功！";
                    } else {
                        // 验签失败
                        msg = "支付失败！";
                    }
                } catch (JSONException e) {
                }
            }
            // 结果result_data为成功时，去商户后台查询一下再展示成功
            msg = "支付成功！";
            Map<String, String> map = new HashMap<>();
//            map.put("sign", sign);
//            map.put("data", dataOrg);
            map.put("result_data", result);
            map.put("SOURCE", "6");
            postRequset(TN_URL, map);
            webview.loadUrl("http://fina.housecenter.cn/cashier/app/notifypage");
        } else if (str.equalsIgnoreCase("fail")) {
            msg = "支付失败！";
        } else if (str.equalsIgnoreCase("cancel")) {
            msg = "用户取消了支付";
        }

        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void run() {
        String tn = null;
        InputStream is;
        try {

            String url = TN_URL;

            URL myURL = new URL(url);
//            URLConnection ucon = myURL.openConnection();
//            ucon.setConnectTimeout(120000);
            HttpURLConnection ucon = (HttpURLConnection) myURL.openConnection();
            ucon.setConnectTimeout(120000);
            ucon.setRequestMethod("POST");
            ucon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            ucon.setRequestProperty("Connection", "Keep-Alive");// 维持长连接
            ucon.setRequestProperty("Charset", "UTF-8");
            Map<String, String> map = new HashMap<>();
            map.put("sign", "R5UuNEJ6X5xlPIqzbahANJQ81WST8JBBThKzZF6/ZI/YWE4EN3U6BcwAk9EBPVdvh9Tl//HVggxtQUa20KoXzKZm/vJm7f7DkAwtObXAuqu5kh3T3Gyvrc7veRZHX2YQ62HYkmWrtXt2tUqNt3Bd03f9ShrDdVoStmex4e3PELuHE/ejWd4sHusxJSvTOHjIeYksB4s14NskEqHVRzHHnKK1muq0+DneQFkqbE7XsYiyKdyKHvk7VKM7f3x0I/385C9x8zxdGJvyzWIpQ1xNy2VbFNAWv+bcEnByr5s5K+hDHJu+Qbb4qRqiUuu8lfaiVxynS/K/osxxRsL+yYrn4Q==");
            map.put("data", "pay_result=success&tn=633189658221127582801&cert_id=69026276696");
            ContentValues values = new ContentValues();
            values.put("result_data", map.toString());
            is = ucon.getInputStream();
            int i = -1;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((i = is.read()) != -1) {
                baos.write(i);
            }

            tn = baos.toString();
            is.close();
            baos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Message msg = mHandler.obtainMessage();
        msg.obj = tn;
        mHandler.sendMessage(msg);
    }

    int startpay(Activity act, String tn, int serverIdentifier) {
        return 0;
    }

    private boolean verify(String msg, String sign64, String mode) {
        // 此处的verify，商户需送去商户后台做验签
        return true;
    }

    @JavascriptInterface
    public void setMessage() {
        Intent intent = new Intent();
        intent.putExtra("result", "OK");
        setResult(PAY_CODE, intent);
        finish();
    }

    @JavascriptInterface
    public void enlargeWebView() {
        myHandler.sendEmptyMessage(0);
        Message msg = new Message();
        myHandler.sendMessage(msg);
    }

    @JavascriptInterface
    public void cloudunionpay(String string) {
        UPPayAssistEx.startPay(PayWebViewActivity.this, null, null, string, mMode);
    }

    public static String getLang(Context context) {
        String lang;
        if (Build.VERSION.SDK_INT >= 24) {
            lang = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
        } else {
            lang = context.getResources().getConfiguration().locale.getLanguage();
        }

        if (TextUtils.equals(lang, "zh")) {
            String country;
            if (Build.VERSION.SDK_INT >= 24) {
                country = context.getResources().getConfiguration().getLocales().get(0).getCountry();
            } else {
                country = context.getResources().getConfiguration().locale.getCountry();
            }

            if (TextUtils.equals(country, "TW")) {
                return "tw";
            }
        }
        return lang;
    }

    private void outAnimation() {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_top_to_bottom);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.setAnimationListener(
                new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        exit(ivClose);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }

                }

        );

        flContent.clearAnimation();
        flContent.startAnimation(animation);
    }

    public void exit(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.finishAfterTransition();
        } else {
            this.finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
        //注释掉activity本身的过渡动画
        overridePendingTransition(0, 0);
    }

    public String postRequset(final String url, final Map<String, String> map) {
        final StringBuilder sb = new StringBuilder();
        FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try {
                    URL requestUrl = new URL(url);
                    connection = (HttpURLConnection) requestUrl.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(10000);//链接超时
                    connection.setReadTimeout(10000);//读取超时
                    //发送post请求必须设置
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setUseCaches(false);
                    connection.setInstanceFollowRedirects(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    DataOutputStream out = new DataOutputStream(connection
                            .getOutputStream());
                    StringBuilder request = new StringBuilder();
                    for (String key : map.keySet()) {
                        request.append(key + "=" + URLEncoder.encode(map.get(key), "UTF-8") + "&");
                    }
                    out.writeBytes(request.toString());//写入请求参数
                    out.flush();
                    out.close();
                    if (connection.getResponseCode() == 200) {
                        InputStream in = connection.getInputStream();
                        reader = new BufferedReader(new InputStreamReader(in));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        System.out.println(sb);

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null) {
                        reader.close();//关闭流
                    }
                    if (connection != null) {
                        connection.disconnect();//断开连接，释放资源
                    }
                }
                return sb.toString();
            }
        });
        new Thread(task).start();
        String s = null;
        try {
            s = task.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }
}
