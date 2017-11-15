package org.hantonr.pr3.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WikiCrawler {

  public static final String BASE_URL = "https://en.wikipedia.org";
  private final Pattern badLinks = Pattern.compile("[:#]+");

  private Map<String, WeightedQueue<String, Double>> queues;

  public WikiCrawler() {
    queues = new HashMap<>();
  }

  public static void main(String[] args) {
    //1. Pick a URL from the frontier
    try {
      WikiCrawler crawler = new WikiCrawler();
      List<String> robots = crawler.getDisallowedRobots();
      crawler.crawl("/wiki/Morpheus_(The_Matrix)", Arrays.asList("matrix"), true, robots, 100);
    } catch (IOException e) {
      System.err.println(e);
    }
  }

  private void crawl(String url, List<String> keywords, boolean isWeighted, List<String> disallowed,
      int max) throws IOException {
    int urlsAnalyzed = 0;
    String parentUrl = url;
    String thisUrl = parentUrl;

    analyzeUrl(thisUrl, keywords, isWeighted);
    LinkedList<String> nextItemsToAnalyze = new LinkedList<>();

    while (queues.size() > 0) {
      WeightedQueue<String, Double> queue = queues.get(parentUrl);

      while (queue.size() > 0 && urlsAnalyzed < max) {
        urlsAnalyzed++;
        thisUrl = queue.extract();
        nextItemsToAnalyze.add(thisUrl);
        System.out.println(parentUrl + " " + thisUrl);
        analyzeUrl(thisUrl, keywords, isWeighted);
      }

      if (urlsAnalyzed >= max) {
        break;
      }

      queues.remove(parentUrl);
      parentUrl = nextItemsToAnalyze.removeFirst();
    }
  }

  private List<String> getDisallowedRobots() throws IOException {
    URL url = new URL(BASE_URL + "/robots.txt");
    URLConnection con = url.openConnection();
    InputStream in = con.getInputStream();
    String encoding = con.getContentEncoding();
    encoding = encoding == null ? "UTF-8" : encoding;

    List<String> robotsLines;
    try (BufferedReader buffer = new BufferedReader(new InputStreamReader(in, encoding))) {
      robotsLines = buffer.lines().collect(Collectors.toList());
    }

    String currentAgent = "";
    List<String> disallowed = new ArrayList<>();
    for (int i = 0; i < robotsLines.size(); i++) {
      if (robotsLines.get(i).toLowerCase().contains("user-agent:")) {
        currentAgent = robotsLines.get(i).substring(11).trim();
      } else if (currentAgent.equals("*") && robotsLines.get(i).toLowerCase().startsWith("disallow")) {
        disallowed.add(robotsLines.get(i).substring(9).trim());
      }
    }
    return disallowed;
  }

  /**
   * Analyze the url text and add weighted (if desired) links to the queue
   */
  public void analyzeUrl(String url, List<String> keywords, boolean isWeighted) throws IOException {
    String urlText = readUrlToString(url);
    urlText = urlText.substring(urlText.indexOf("<p"));

    WeightedQueue<String, Double> queue = new WeightedQueue<>();
    queues.put(url, queue);

    List<Href> links = getPageLinks(urlText);
    for (Href link : links) {
      // ensure we do not deal with links with : or #
      if (link.getAttributes().get("href") != null &&
          link.getAttributes().get("href").startsWith("/wiki/") &&
          !badLinks.matcher(link.getAttributes().get("href")).find()) {
        double distance = getDistanceOfLink(urlText, link, keywords);
        if (isWeighted) {
          queue.add(link.getAttributes().get("href"), distance);
        } else {
          queue.add(link.getAttributes().get("href"));
        }
      }
    }
  }

  public List<Href> getPageLinks(String urlText) {
    List<Href> links = new ArrayList<>();

    Matcher matcher = Pattern.compile("<a .*?>(.*?)</a>").matcher(urlText);
    while(matcher.find()) {
      links.add(new Href(matcher.start(), matcher.end(),
          urlText.substring(matcher.start(), matcher.end())));
    }

    return links;
  }

  public double getDistanceOfLink(String originalText, Href href, List<String> targets) {

    // check out href attributes for target words
    for (String target : targets) {
      String targetLowercase = target.toLowerCase();
      if (href.html().toLowerCase().contains(targetLowercase)
          || href.attr("href").toLowerCase().contains(targetLowercase)) {
        return 1.0d;
      }
    }

    int distance = 0;
    String leftWord, rightWord;
    int leftPos = href.getStart();
    int rightPos = href.getEnd();
    // get leftWord & rightWord
    while (distance <= 20) {
      distance++;
      leftWord = getNextLeftWord(originalText, leftPos);
      rightWord = getNextRightWord(originalText, rightPos);
      for (String target : targets) {
        String targetLowercase = target.toLowerCase();
        if (rightWord.toLowerCase().contains(targetLowercase)
            || leftWord.toLowerCase().contains(targetLowercase)) {
          return 1.0d / (distance + 2);
        }
      }

      leftPos = leftPos - leftWord.length() - 1;
      rightPos = rightPos + rightWord.length() + 1;
    }

    // start searching nearby text
    return 0d;
  }

  private String getNextLeftWord(String text, int startingPos) {
    int end = indexOfNextSpace(text, false, startingPos);
    int start = indexOfNextSpace(text, false, end);

    if (end < 0) {
      return "";
    }
    return text.substring(start + 1, end);
  }

  private String getNextRightWord(String text, int startingPos) {
    int start = indexOfNextSpace(text, true, startingPos-1);
    int end = indexOfNextSpace(text, true, start);

    if (end < 0) {
      return "";
    }
    return text.substring(start + 1, end);
  }

  private int indexOfNextSpace(String string, boolean moveRight, int startingPos) {
    int index = startingPos;
    if (moveRight && index < string.length()) {
      index++;
    } else if (!moveRight && index > 0) {
      index--;
    } else {
      return -1;
    }

    while (string.charAt(index) != ' ') {
      if (moveRight && index < string.length()) {
        index++;
      } else if (!moveRight && index > 0) {
        index--;
      } else {
        return -1;
      }
    }
    return index;
  }

  private String readUrlToString(String urlString) throws IOException {
    if (urlString == null || urlString.isEmpty()) {
      throw new IllegalArgumentException("Invalid url");
    }

    URL url = new URL(BASE_URL + urlString);
    URLConnection con = url.openConnection();
    InputStream in = con.getInputStream();
    String encoding = con.getContentEncoding();  // ** WRONG: should use "con.getContentType()" instead but it returns something like "text/html; charset=UTF-8" so this value must be parsed to extract the actual encoding
    encoding = encoding == null ? "UTF-8" : encoding;

    try (BufferedReader buffer = new BufferedReader(new InputStreamReader(in, encoding))) {
      return buffer.lines().collect(Collectors.joining("\n"));
    }
  }

  private class Href {
    int start;
    int end;
    String text;
    Map<String, String> attributes = new HashMap<>();
    String html;

    public Href(int start, int end, String text) {
      this.start = start;
      this.end = end;
      this.text = text;
      populateAttributes();
      populateHtml();
    }

    private void populateAttributes() {
      Pattern p = Pattern.compile("(\\S+)=[\"']?((?:.(?![\"']?\\s+(?:\\S+)=|[>\"']))+.)[\"']?");
      Matcher matcher = p.matcher(this.text);
      while(matcher.find()) {
        String thisGroup = matcher.group();
        String[] keyValue = thisGroup.split("=", 2);
        this.attributes.put(keyValue[0], keyValue[1].replace("\"", ""));
      }
    }

    private void populateHtml() {
      Pattern p = Pattern.compile("<a .*?>(.*?)</a>");
      Matcher m = p.matcher(this.text);
      if (m.find()) {
        this.html = m.group(1);
      }
    }

    public int getStart() {
      return start;
    }

    public int getEnd() {
      return end;
    }

    public String getText() {
      return text;
    }

    public Map<String, String> getAttributes() {
      return attributes;
    }

    public String attr(String key) {
      String attribute = attributes.get(key);
      return attribute == null ? "" : attribute;
    }

    public String html() {
      return html;
    }
  }
}