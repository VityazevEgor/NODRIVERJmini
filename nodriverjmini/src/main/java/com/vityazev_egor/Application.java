package com.vityazev_egor;

import java.io.IOException;
import java.util.Scanner;
import javax.imageio.*;

import com.vityazev_egor.Core.WaitTask;
import com.vityazev_egor.Core.WebElements.By;

import java.nio.file.Path;

public class Application {

    public static void main(String[] args) throws IOException, InterruptedException {
        exampleCopilotAuth();
    }

    public static void exampleCopilotAuth() throws IOException{
        NoDriver driver = new NoDriver();
        driver.getXdo().calibrate();
        driver.getNavigation().loadUrlAndWait("https://ya.ru", 10);
        var input = driver.findElement(By.id("text"));
        input.getPosition().ifPresent(point -> driver.getXdo().click(point.getX(), point.getY()));
        waitEnter();
        driver.exit();
    }

    private static Boolean copilotAuth(NoDriver driver) {
        driver.getNavigation().loadUrlAndWait("https://copilot.microsoft.com/", 10);

        // Ожидаем и нажимаем на первую кнопку "Sign in"
        var signInButton = driver.findElement(By.cssSelector("button[title=\"Sign in\"]"));
        var waitForSignInButton = new WaitTask() {
            @Override
            public Boolean condition() {
                return signInButton.isExists();
            }
        };
        if (!waitForSignInButton.execute(5, 400)) {
            System.out.println("Can't find sign in button");
            return false;
        }
        driver.getInput().emulateClick(signInButton);

        // Проверяем наличие второй кнопки "Sign in" после раскрытия меню
        // var signInButtons = driver.findElements(By.cssSelector("button[title=\"Sign in\"]"));
        // if (signInButtons.size() < 2) {
        //     System.out.println("There are less than 2 'Sign in' buttons - " + signInButtons.size());
        //     return false;
        // }
        // driver.getInput().emulateClick(signInButtons.get(1));

        // Ожидаем появления поля для ввода логина
        var loginInput = driver.findElement(By.name("loginfmt"));
        var waitForLoginInput = new WaitTask() {
            @Override
            public Boolean condition() {
                return loginInput.isExists();
            }
        };
        if (!waitForLoginInput.execute(5, 400)) {
            System.out.println("Can't find login input");
            return false;
        }

        // Вводим email и нажимаем кнопку "Далее"
        driver.getInput().enterText(loginInput, "test@gmail.com");
        System.out.println("Writing text");
        var loginButton = driver.findElement(By.id("idSIButton9"));
        if (loginButton.isExists()) {
            driver.getInput().emulateClick(loginButton);
        }

        return true;
    }

    public static void waitEnter(){
        var sc = new Scanner(System.in);
        sc.nextLine();
    }
}
