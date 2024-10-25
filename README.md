# NODRIVERJmini
 A library for controlling the browser directly via web sockets without using web drivers

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

## Example usage 
```Java
public static void exampleCopilotAuth() throws IOException{
        NoDriver driver = new NoDriver("127.0.0.1:2080");
        System.out.println("Auth result = " + copilotAuth(driver));
        waitEnter();
        driver.exit();
    }

    private static Boolean copilotAuth(NoDriver driver) {
        driver.getNavigation().loadUrlAndWait("https://copilot.microsoft.com/", 10);

        // We wait and click on the first "Sign in" button
        var signInButton = driver.findElement(By.cssSelector("button[title=\"Sign in\"]"));
        var waitForSignInButton = new WaitTask() {
            @Override
            public Boolean condition() {
                return signInButton.isExists(driver);
            }
        };
        if (!waitForSignInButton.execute(5, 400)) {
            System.out.println("Can't find sign in button");
            return false;
        }
        driver.getInput().emulateClick(signInButton);

        // We check the presence of the second "Sign in" button after opening the menu
        var signInButtons = driver.findElements(By.cssSelector("button[title=\"Sign in\"]"));
        if (signInButtons.size() < 2) {
            System.out.println("There are less than 2 'Sign in' buttons - " + signInButtons.size());
            return false;
        }
        driver.getInput().emulateClick(signInButtons.get(1));

        // We are waiting for the login field to appear
        var loginInput = driver.findElement(By.name("loginfmt"));
        var waitForLoginInput = new WaitTask() {
            @Override
            public Boolean condition() {
                return loginInput.isExists(driver);
            }
        };
        if (!waitForLoginInput.execute(5, 400)) {
            System.out.println("Can't find login input");
            return false;
        }

        // Enter the email and click the "Next" button
        driver.getInput().enterText(loginInput, "test@gmail.com");
        var loginButton = driver.findElement(By.id("idSIButton9"));
        if (loginButton.isExists(driver)) {
            driver.getInput().emulateClick(loginButton);
        }

        return true;
    }

```