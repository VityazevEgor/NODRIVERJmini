# NODRIVERJmini
 A library for controlling the browser directly via web sockets without using web drivers, which give you ability to be undectable for anti-bot systems.

 **Features**
 * Do not need a web-driver
 * Works with any version of chrome browser
 * Pass anti-bot [checks](https://bot.sannysoft.com/)
 * Simular to Selenium
 * Can emulate mouse input on system level (xdotool)
 * Supports Windows and Linux operating systems

 **System Requirements**
 * For Linux users: Install xdotool for XDO class functionality (system-level mouse emulation)
   ```bash
   sudo apt install xdotool
   ```

 > A idea of my library was inspired by [nodriver](https://github.com/ultrafunkamsterdam/nodriver), but it's not a complete java fork of this library.

## How to install

### Step 1. Run the Installation Script

In your command line, run the following command to execute `install.sh`, which installs the library to your local Maven repository:

```bash
bash install.sh
```

### Step 2. Add the Dependency to Your `pom.xml`

Once the installation is complete, add the following dependency to your project’s `pom.xml` file:

```xml
<dependency>
    <groupId>com.vityazev_egor</groupId>
    <artifactId>nodriverjmini</artifactId>
    <version>1.0</version>
</dependency>
```

The NODRIVERJmini is now ready to use in your project!

```Java
NoDriver driver = new NoDriver();
```
## CloudFlare bypass
![CF bypass](/images/cfBypassXDO.gif "CF bypass")

Here's everything you need to write in order to bypass CloudFlare by using XDO:
```java
NoDriver d = new NoDriver("127.0.0.1:2080");
        d.getXdo().calibrate();
        Boolean result = d.getNavigation().loadUrlAndBypassCFXDO("https://dstatlove.ink/hit", 5, 30);
        d.exit();
        assertTrue(result);
```

You can also bypass CloudFlare without emulating real mouse input, but it works **only if you have clear ip**:
```java
NoDriver d = new NoDriver();
        Boolean result = d.getNavigation().loadUrlAndBypassCFCDP("https://nopecha.com/demo/cloudflare", 10, 30);
        d.exit();
        assertTrue(result);
```


## Example code
Here is example of basic usage that searches for "NODRIVERJmini" on Google:

```Java
public void exampleUsage() {
    try {
        // Initialize a new NoDriver instance
        NoDriver driver = new NoDriver();

        // Load the URL "https://google.com" and wait for it to fully load
        driver.getNavigation().loadUrlAndWait("https://google.com", 10);

        // Find the textarea element with the specified CSS selector
        WebElement textArea = driver.findElement(By.cssSelector("textarea[maxlength='2048']"));

        // Wait for the textarea to appear on the page
        textArea.waitToAppear(5, 200);

        // Insert text "NODRIVERJmini" into the textarea
        driver.getInput().insertText(textArea, "NODRIVERJmini");

        // Pause for 100 milliseconds
        Shared.sleep(100);

        // Find all suggestion elements with the specified CSS selector
        List<WebElement> suggestions = driver.findElements(By.cssSelector("li[data-attrid='AutocompletePrediction']"));

        // Check if any suggestions were found
        if (suggestions.isEmpty())
            throw new Exception("Can't find suggestions");

        // Print each suggestion's text
        suggestions.forEach(suggestion -> suggestion.getText().ifPresent(System.out::println));

        // Emulate a click on the first suggestion
        driver.getInput().emulateClick(suggestions.getFirst());

        // Pause for 50 milliseconds
        Shared.sleep(50);

        // Wait for the navigation to fully load
        driver.getNavigation().waitFullLoad(5);

        // Capture a screenshot and save it as "search_results.png"
        driver.getMisc().captureScreenshot(Path.of("search_results.png"));

        // Exit the NoDriver instance
        driver.exit();
    } catch (IOException ex){
        System.err.println("Can't init chrome: " + ex.getMessage());
        System.exit(1);
    } catch (Exception e) {
        System.err.println("Can't find element(s):" + e.getMessage());
        System.exit(1);
    }
    System.exit(0);
}
```