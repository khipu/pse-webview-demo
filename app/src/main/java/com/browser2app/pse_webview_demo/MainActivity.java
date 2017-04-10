package com.browser2app.pse_webview_demo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.SslError;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = MainActivity.class.getSimpleName();
	private WebView webView;
	private EditText urlField;
	private ProgressBar progressBar;

	private String startUrl = "https://www.psepagos.co/PSEHostingUI/ShowTicketOffice.aspx?ID=4748";

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItemCompat.setShowAsAction(
				menu.add(Menu.NONE, R.id.clear, Menu.NONE, "clear").setIcon(R.drawable.ic_clear)
				, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		MenuItemCompat.setShowAsAction(
				menu.add(Menu.NONE, R.id.reload, Menu.NONE, "refresh").setIcon(R.drawable.ic_reload)
				, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			stop();
			return true;
		} else if (item.getItemId() == R.id.clear) {
			stop();
			urlField.setText("");
			urlField.requestFocus();
			return true;
		} else if (item.getItemId() == R.id.reload) {
			webView.loadUrl(startUrl);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private void go() {
		String url = urlField.getText().toString().toLowerCase();
		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			url = "http://" + url;
		}

		urlField.setText(url);
		webView.loadUrl(url);
		webView.requestFocus();
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(urlField.getWindowToken(), 0);
	}

	private void stop() {
		webView.stopLoading();
	}

	@Override
	public void onBackPressed() {
		if (webView.canGoBack()) {
			webView.goBack();
		} else {
			super.onBackPressed();
		}
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(myToolbar);

		TextView version = (TextView) findViewById(R.id.version);
		PackageInfo pInfo = null;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			version.setText("Version: " + pInfo.versionName);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}



		urlField = (EditText) findViewById(R.id.urlField);
		progressBar = (ProgressBar) findViewById(R.id.progress);

		webView = (WebView) findViewById(R.id.webview);

		webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
		webView.getSettings().setLoadWithOverviewMode(true);
		webView.getSettings().setUseWideViewPort(true);
		webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setDisplayZoomControls(false);
		webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 6.0.1; ONE A2001 Build/MMB29M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/55.0.2883.91 Mobile Safari/537.36");

		webView.setWebViewClient(new WebViewClient() {

			@Override
			public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setMessage("Certificado SSL inválido " +  error.getCertificate().toString());
				builder.setPositiveButton("continue", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						handler.proceed();
					}
				});
				builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						handler.cancel();
					}
				});
				final AlertDialog dialog = builder.create();
				dialog.show();
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {

				if (url.startsWith("http")) {
					urlField.setText(url);
					return false;
				}
				Uri parsedUri = Uri.parse(url);
				PackageManager packageManager = getPackageManager();

				//Intentar ejecutar el intent directamente si el package manager es capaz de resolverlo
				Intent browseIntent = new Intent(Intent.ACTION_VIEW).setData(parsedUri);
				if (browseIntent.resolveActivity(packageManager) != null) {
					startActivity(browseIntent);
					return true;
				}

				//Si el package manager no pudo obtenerlo intentar parsear un intent de tipo "intent://"
				if (url.startsWith("intent:")) {
					try {
						Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
						if (intent.resolveActivity(getPackageManager()) != null) {
							startActivity(intent);
							return true;
						}

						//Usar la url de fallback
						String fallbackUrl = intent.getStringExtra("browser_fallback_url");
						if (fallbackUrl != null) {
							webView.loadUrl(fallbackUrl);
							return true;
						}

						//Invitar a instalar
						Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(
								Uri.parse("market://details?id=" + intent.getPackage()));
						if (marketIntent.resolveActivity(packageManager) != null) {
							startActivity(marketIntent);
							return true;
						}
					} catch (URISyntaxException e) {
						//not an intent uri
					}
				}
				return true;//do nothing in other cases
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				if (url.startsWith("http://") || url.startsWith("https://")) {
					urlField.setText(url);
				}

				progressBar.setVisibility(View.GONE);
				webView.requestFocus();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
			}
		});

		webView.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				Log.d(TAG, "Progress " + newProgress);
				progressBar.setVisibility(View.VISIBLE);
				progressBar.setProgress(newProgress);
				super.onProgressChanged(view, newProgress);
			}
		});

		webView.getSettings().setJavaScriptEnabled(true);

		webView.loadUrl(startUrl);


		urlField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_GO) {
					go();
					handled = true;
				}
				return handled;
			}
		});

		urlField.setSelectAllOnFocus(true);

		urlField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View view, boolean hasFocus) {
				if (hasFocus) {
					view.setBackgroundResource(R.drawable.rect_transparent);
				} else {
					view.setBackgroundResource(R.drawable.browser_bar_bg);
				}
			}
		});


	}
}
