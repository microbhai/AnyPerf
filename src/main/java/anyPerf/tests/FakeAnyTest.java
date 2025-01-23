package anyPerf.tests;

/*
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.XYZ.performance.SelPerf.utility.Driver;
import com.XYZ.performance.SelPerf.utility.Wait;

public class FakeAnyTest extends AnyTest {

	WebDriver driver;
	@Test
	public void test() {
		try {
			startTransaction("Test1_Step1");
			step1();
			endTransaction("Test1_Step1");

			startTransaction("Test1_Step2");
			step2();
			endTransaction("Test1_Step2");

			startTransaction("Test1_Step3");
			step3();
			endTransaction("Test1_Step3");

			startTransaction("Test1_Step4");
			step4();
			endTransaction("Test1_Step4");
			
			startTransaction("Test1_Step5");
			step5();
			endTransaction("Test1_Step5");
			
			startTransaction("Test1_Step6");
			step6();
			endTransaction("Test1_Step6");

		} catch (Exception e) {
			System.out.println("Exception in test 1 execution");
			e.printStackTrace();
		} finally {
			// System.out.println("Test1");
		}
	}

	public void step1() throws Exception {
		// System.out.println("Test1_Step1");
		thinkTime(randomBetween(1000l, 2000l));
		driver = new Driver().startDriver();
		
		driver.navigate().to("https://XYZ-qa.oktapreview.com/login/default");
		
		driver.findElement(By.xpath("//*[@id=\"okta-signin-username\"]")).sendKeys("akhil.sharma@XYZ.com");
		driver.findElement(By.id("okta-signin-password")).sendKeys("Parc9102_");;
		driver.findElement(By.cssSelector("#okta-signin-submit")).click();;
	}

	public void step2() throws Exception {
		// System.out.println("Test1_Step2");
		thinkTime(randomBetween(2000l, 3000l));
		Thread.sleep(3000);
		Wait.waitUntilElementVisible(driver, By.cssSelector("a[href*='successfactorsXYZrlsqa_1']"));
		driver.findElement(By.cssSelector("a[href*='successfactorsXYZrlsqa_1']")).click();
	}

	public void step3() throws Exception {
		// System.out.println("Test1_Step3");
		thinkTime(randomBetween(3000l, 4000l));
		Set<String> tabIds = driver.getWindowHandles();
		Iterator<String> it = tabIds.iterator();
		String parentID = it.next();
		String childID = it.next();
		driver.switchTo().window(childID);
	}

	public void step4() throws Exception {
		// System.out.println("Test1_Step4");
		thinkTime(randomBetween(4000l, 5000l));
		Wait.waitUntilElementVisible(driver, By.xpath("//img[@title='Akhil Sharma']"));
		driver.findElement(By.id("utilityLinksMenuId")).click();
		
		Wait.waitUntilElementVisible(driver,By.xpath("//a[contains(text(),'Log out')]"));
		driver.findElement(By.xpath("//*[@id=\"__item7-__list2-0\"]")).click();
		
		WebElement targetUser = driver.findElement(By.id("__input0-inner"));
		targetUser.sendKeys("HR Direct Admin");
		Wait.waitUntilElementVisible(driver, By.className("surjcontent"));
		for (int i = 0; i < 2; i++) {
			targetUser.sendKeys(Keys.ARROW_DOWN);
		}
		targetUser.sendKeys(Keys.ENTER);
		driver.findElement(By.id("__button12-content")).click();
		}
	
	public void step5() throws Exception {
		// System.out.println("Test1_Step5");
		thinkTime(randomBetween(5000l, 6000l));
		Wait.waitUntilElementVisible(driver, By.xpath("//img[@title='HR Direct Admin']"));
		driver.findElement(By.xpath("//div[@id='__tile4']")).click();
		Wait.waitUntilElementVisible(driver, By.xpath("//a[@title='Compensation Home']"));
		driver.findElement(By.xpath("//a[@title='Compensation Home']")).click();
	}
	
	public void step6() throws Exception {
		// System.out.println("Test1_Step6");
		thinkTime(randomBetween(7000l, 8000l));
		driver.findElement(By.id("utilityLinksMenuId-inner")).click();
		Wait.waitUntilElementVisible(driver, By.xpath("//a[contains(text(),'Log out')]")).click();
		driver.quit();
	}
	
}
*/
