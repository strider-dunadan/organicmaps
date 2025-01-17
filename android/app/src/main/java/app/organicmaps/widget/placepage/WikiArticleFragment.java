package app.organicmaps.widget.placepage;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmFragment;
import app.organicmaps.util.Utils;

public class WikiArticleFragment extends BaseMwmFragment
{
  public static final String EXTRA_WIKI_ARTICLE = "description";
  public static final String EXTRA_WIKI_URL = "wiki_url";

  @NonNull
  private String mDescription = "";
  @NonNull
  private String mWikiUrl = "";
  private WebView mWebView;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    mDescription = requireArguments().getString(EXTRA_WIKI_ARTICLE, "");
    mWikiUrl = requireArguments().getString(EXTRA_WIKI_URL, "");
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState)
  {
    View root = inflater.inflate(R.layout.fragment_place_description, container, false);
    mWebView = root.findViewById(R.id.webview);
    mWebView.setVerticalScrollBarEnabled(true);
    mWebView.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url)
      {
        Utils.openUrl(requireContext(), url);
        return true;
      }
    });
    mWebView.getSettings().setDefaultTextEncodingName("utf-8");
    ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
      Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
      loadDescription(insets.bottom);
      return windowInsets;
    });
    return root;
  }

  private void loadDescription(int bottomInsetPx)
  {
    String source = mWikiUrl.isEmpty()
                      ? "<p>" + getString(R.string.article_from_wikipedia) + "</p>"
                      : "<p><a href='" + mWikiUrl + "'>" + getString(R.string.article_from_wikipedia) + "</a></p>";

    String bodyClass = isDarkMode() ? "dark" : "";

    int bottomPaddingCssPx = (int) (bottomInsetPx / getResources().getDisplayMetrics().density);
    String html =
        "<!DOCTYPE html><html><head>"
        + "<meta charset='utf-8'>"
        + "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>"
        + "<link rel='stylesheet' href='wikipedia.css'>"
        + "<style>body { padding-bottom: " + bottomPaddingCssPx + "px; }</style>"
        + "</head><body class='" + bodyClass + "'>" + mDescription + source + "</body></html>";
    mWebView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
  }

  private boolean isDarkMode()
  {
    int nightFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    return nightFlags == Configuration.UI_MODE_NIGHT_YES;
  }
}
