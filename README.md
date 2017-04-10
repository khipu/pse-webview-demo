# DEMO invocación de PSE Móvil desde un Webview en Android

Versión 1.0

El objetivo de esta aplicación es demostrar la integración de una aplicación Android basada en Webviews con la aplicación de pagos PSE móvil.

Los puntos relevantes de la app son:

1. Utilizar un user agent reconocido como de Android estándar. En el ejemplo se utiliza el de Chrome 55

		webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 6.0.1; ONE A2001 Build/MMB29M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/55.0.2883.91 Mobile Safari/537.36");

2. Soportar las URLs de tipo intent:// 

	webView.setWebViewClient(new WebViewClient() {
	
		...

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
	
			...
			
			Uri parsedUri = Uri.parse(url);
			PackageManager packageManager = getPackageManager();
			Intent browseIntent = new Intent(Intent.ACTION_VIEW).setData(parsedUri);
			
			//Intentar ejecutar el intent directamente si el package manager es capaz de resolverlo
			
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
					//No es una url de intent
				}
			}
			return true;//no hacer nada
		}
		...
	}


