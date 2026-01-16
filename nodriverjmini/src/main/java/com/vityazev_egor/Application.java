package com.vityazev_egor;

import java.io.IOException;
import java.util.Scanner;
import javax.imageio.*;

import com.vityazev_egor.Core.WaitTask;
import com.vityazev_egor.Core.WebElements.By;

import java.nio.file.Path;

public class Application {

    public static void main(String[] args) throws IOException, InterruptedException {
        NoDriver driver = new NoDriver();
        driver.getXdo().calibrate();
        driver.getNavigation().loadUrlAndWait("https://pastebin.com/", 10);
        var input = driver.findElement(By.id("postform-text"));
        String code = """
                for i in range (0,10):
                    print (i)
                """;
        driver.getInput().insertText(input, code);
        waitEnter();
        driver.exit();
    }

    public static void waitEnter(){
        var sc = new Scanner(System.in);
        sc.nextLine();
    }
}
