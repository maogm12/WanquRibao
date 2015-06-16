package com.maogm.wanquribao.Fetcher;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.maogm.wanquribao.Module.Issue;
import com.maogm.wanquribao.Module.IssueItem;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

interface IssuesFetchedListner {
    public void OnIssueFetched(Issue issue);
}


/**
 * IssuesFetcher
 * @author: Guangming Mao
 */
public class IssuesFetcher extends AsyncTask<String, Integer, Issue> {
    private final static String wanquRoot = "http://wanqu.co";
    private final static String wanquIssueRoot = "http://wanqu.co/issues";

    private IssuesFetchedListner issuesFetchedListner;

    private String requestIssues(String issuesUrl) {
        if (!issuesUrl.startsWith(wanquRoot)) {
            return "";
        }

        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(issuesUrl);
        try {
            HttpResponse response = client.execute(request);
            InputStream in = response.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder str = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null)
            {
                str.append(line);
            }
            in.close();
            return str.toString();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("IssueFetcher", "Fetch issues error");
            return "";
        }
    }

    private Issue parseIssue(String html) {
        Document doc = Jsoup.parse(html);

        Issue issue = new Issue();

        // heading
        Elements heading = doc.select(".panel-heading > h1");
        if (heading == null || heading.first() == null) {
            return null;
        }
        Element titleElem = heading.first();
        String title = titleElem.text();
        Pattern pattern = Pattern.compile("((\\d+)/(\\d+)/(\\d+)) 第(\\d+)期");
        Matcher m = pattern.matcher(title);
        if (!m.matches()) {
            Log.e("IssueFetcher", "Parse title error");
            return null;
        }
        String dateStr = m.group(1);
        String numStr = m.group(5);
        try
        {
            // time
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            issue.time = sdf.parse(dateStr);

            // number
            issue.number = Integer.parseInt(numStr);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
            return null;
        }


        // content
        Elements items = doc.select(".list-group-item");
        for (Element issueItemElem : items) {
            IssueItem item = parseIssueItem(issueItemElem);
            issue.issues.add(item);
        }

        return issue;
    }

    private IssueItem parseIssueItem(Element issueItemElem) {
        if (issueItemElem == null) {
            return null;
        }

        IssueItem item = new IssueItem();

        // head
        Elements titleElems = issueItemElem.getElementsByClass("lead");
        if (titleElems != null && titleElems.size() > 0) {
            Element titleElem = titleElems.first();
            item.link = titleElem.attr("href");
            item.title = titleElem.text();
        }

        // content
        Elements contentElems = issueItemElem.getElementsByTag("p");
        if (contentElems != null && contentElems.size() > 0) {
            Element contentElem = contentElems.first();
            item.brief = contentElem.text();
        }

        // tags
        Elements tagElems = issueItemElem.getElementsByClass("label");
        if (tagElems != null) {
            for (int i = 0; i < tagElems.size(); ++i) {
                item.tags.add(tagElems.get(i).text());
            }
        }

        return item;
    }

    @Override
    protected Issue doInBackground(String... params) {
        String issuesUrl = wanquRoot;
        if (params.length > 0) {
            issuesUrl = params[0];
        }

        String html = requestIssues(issuesUrl);
        if (TextUtils.isEmpty(html)) {
            return null;
        }
        return parseIssue(html);
    }
}
