package com.example.hwmbot.HWMService;

import com.example.hwmbot.helpers.*;
import matchParser.MatchParser;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HWMService {
    public static int GL_LEVEL_LOW = 0;
    public static int GL_LEVEL_MIDDLE = 1;
    public static int GL_LEVEL_HIGH = 2;

    private static String TEXT_OK_CODE = "Вы устроены на работу";
    private static String TEXT_INCORRECT_CODE = "Введен неправильный код";
    private static String TEXT_BAD_GOOGLE_TOKEN = "Вы должны поставить галочку";
    private static String TEXT_NOT_FREE_SLOT = "Нет рабочих мест";
    private static String TEXT_NOT_RESOURCES = "На объекте недостаточно";
    private static String TEXT_END_FIGHT = "Победившая сторона";

    private static String ARROW = "&raquo;&raquo;&raquo;";
    private static String REG_WORK_FORM = "form\\sname=work.+?action=['\"](.+?)['\"].+?/form";
    private static String REG_NUM_EXPRESSION = "num['\"]\\)\\.value\\s*=\\s*(.+?);";
    private static String REG_CAPTCHA = "work_codes.+?jpeg";
    private static String REG_RE_CAPTCHA = "data-sitekey=[\"'](.+?)[\"']";
    private static String REG_HIDDEN_INPUT = "<input\\stype=hidden.+?>";
    private static String REG_HEAD = "<head.+?/head>";
    private static String REG_RE_CAPTCHA_SCRIPT = "<script\\ssrc='//www\\.google\\.com/.+?</script>";
    private static String REG_FINE_WORKAHOLIC = "\\*(0\\.\\d)\\s\\(штраф";
    private static String REG_URL_AUTO_FIGHT = "href=\"(" + HWMRequests.MAP_ADDRESS + "\\?action=attack\\d?&auto=1.+?)\"><b>Автобой";
    private static String REG_ANIMAL_HUNTING = "army_info.php\\?name=.+?'>(.+?)</a>\\s\\((.+?)\\s.+?(\\d*)\\sзолота";
    private static String REG_STATE_JOB = "Место работы:.+?с\\s([\\d:]*)";
    private static String REG_STATE_JOB2 = "место работы:.+?(\\d+)\\sмин";
    private static String REG_ID_COMPANY = "object-info\\.php\\?id=(\\d*).+?<b>(\\d*)<\\/b>.+?none;'>" + ARROW;

    private int captchaRequestHour;
    private String companyId;
    private String companyPage;
    private HWMRequests hwmRequests;
    private HeroInfo heroInfo = new HeroInfo();
    private StandardCodes standardCodes = new StandardCodes();

    public HWMService(String login, String password, String cookies, StandardCodes standardCodes) {
        this.hwmRequests = new HWMRequests(login, password, cookies);
        this.standardCodes = standardCodes;
    }

    public HWMService(String login, String password, String cookies) {
        this.hwmRequests = new HWMRequests(login, password, cookies);
    }

    public HWMService(String login, String password){
        this.hwmRequests = new HWMRequests(login, password, null);
    }

    public String getCookie() {
        return hwmRequests.getCookie();
    }

    public String getStatusAuthorization() {
        String statusLogin = "OK";

        try {
            hwmRequests.getPage(hwmRequests.HOME_ADDRESS);
        } catch (IOException ex) {
            statusLogin = ex.getMessage();
        }

        return statusLogin;
    }

    public HeroInfo getHeroInfo() {
        return heroInfo;
    }

    public String goToHome() throws IOException {
        String homePage = hwmRequests.getPage(HWMRequests.HOME_ADDRESS).responseText;
        updateInformationFromHomePage(homePage);

        return homePage;
    }

    public String goToMap() throws IOException {
        String mapPage = hwmRequests.getPage(HWMRequests.MAP_ADDRESS).responseText;
        companyId = getCompanyId(mapPage);
        heroInfo.animalsHunting = getAnimalsHunting(mapPage);

        return mapPage;
    }

    public String goToCompany() throws HWMException, IOException {
        if (companyId != null) {
            companyPage = hwmRequests.getPage("object-info.php?id=" + companyId).responseText;
            updateInformationFromCompanyPage(companyPage);
        } else {
            companyPage = null;
            throw new HWMException(HWMException.NOT_FREE_SLOT);
            // Нет мест в секторе, значит надо искать предприятие которое освободится раньше остальных
        }

        return companyPage;
    }

    public void getJob(String code) throws HWMException, IOException, Exception {
        Matcher matcherForm = Pattern.compile(REG_WORK_FORM).matcher(companyPage);
        int currentHour = new GregorianCalendar().get(Calendar.HOUR_OF_DAY);

        if (currentHour != captchaRequestHour) {
            throw new HWMException(HWMException.OLD_CAPTCHA);
        }

        if (matcherForm.find()) {
            String formCode = matcherForm.group();
            String urlPath = matcherForm.group(1);
            Map params = Parser.form(formCode);
            Matcher matcherExpression = Pattern.compile(REG_NUM_EXPRESSION).matcher(formCode);

            if (matcherExpression.find()) {
                double resultExpression = new MatchParser().Parse(matcherExpression.group(1));
                params.put("code", code);
                params.put("num", new DecimalFormat("#").format(resultExpression));
            } else {
                params.put("g-recaptcha-response", code);
            }

            RequestInfo requestInfo = hwmRequests.send(urlPath, params);
            String response = requestInfo.responseText;

            if (response.indexOf(TEXT_INCORRECT_CODE) != -1 || response.indexOf(TEXT_BAD_GOOGLE_TOKEN) != -1) {
                throw new HWMException(HWMException.INCORRECT_CODE);
            } else if (response.indexOf(TEXT_NOT_FREE_SLOT) != -1 || response.indexOf(TEXT_NOT_RESOURCES) != -1) {
                goToMap();
                goToCompany();
                getJob(code);
            } else if (response.indexOf(TEXT_OK_CODE) == -1) {
                Log.append("sdcard/error:" + new GregorianCalendar().getTime() +".txt", response);
                throw new HWMException(HWMException.FAILED_GET_JOB);
            }
        } else {
            throw new HWMException(HWMException.FAILED_GET_JOB);
        }
    }

    public void goGuildLeaders(int levelDifficulty) {
        try {
            String REG_FORM_GL = "<form\\s name=\"f.+?/form>";
            String glPage = hwmRequests.getPage(hwmRequests.GL_ADDRESS).responseText;
            Matcher formGLMatcher = Pattern.compile(REG_FORM_GL).matcher(glPage);
            for (int currentLevel = 0; formGLMatcher.find(); currentLevel++) {
                if (currentLevel == levelDifficulty) {
                    hwmRequests.send(hwmRequests.GL_ADDRESS, Parser.form(formGLMatcher.group()));
                    // Зашли в бой, авторасстановка, начать бой, автобой, слежение за состояниями, ожидание конца боя.
                }
            }
        } catch (IOException ex) {}
    }

    public void skipHunting() throws IOException {
        hwmRequests.getPage(hwmRequests.MAP_ADDRESS + "?action=skip");
    }

    public boolean goHunting(int indexAnimal) throws Exception {
        boolean attackIsHappened = false;
        String urlGoHunting = null;
        String mapPage = goToMap();
        Matcher matcherAutoFight = Pattern.compile(REG_URL_AUTO_FIGHT).matcher(mapPage);

        for (int currentIndex = 0; matcherAutoFight.find(); currentIndex++) {
            if (currentIndex == indexAnimal) {
                urlGoHunting = matcherAutoFight.group(1);
                break;
            }
        }

        if (urlGoHunting != null) {
            RequestInfo requestInfo = hwmRequests.getPage(urlGoHunting);

            if (requestInfo.connection.getURL().toString().indexOf(HWMRequests.WAITING_RESULT) != -1) {
                while (true) {
                    if (requestInfo.responseText.indexOf(TEXT_END_FIGHT) != -1) {
                        hwmRequests.getPage(HWMRequests.WAITING_RESULT + "?exit=1");
                        attackIsHappened = true;
                        break;
                    } else {
                        Thread.sleep(5000);
                        requestInfo = hwmRequests.getPage(HWMRequests.WAITING_RESULT);
                    }
                }
            }
        }

        return attackIsHappened;
    }

    private void updateInformationFromHomePage(String homePage) {
        Matcher matcher = Pattern.compile(REG_STATE_JOB).matcher(homePage);
        Matcher matcher2 = Pattern.compile(REG_STATE_JOB2).matcher(homePage);
        Calendar endShift = new GregorianCalendar();

        if (matcher.find()) {
            String timeHm = matcher.group(1);
            String[] timeHmSplit = timeHm.split(":");
            endShift.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeHmSplit[0]) + 1);
            endShift.set(Calendar.MINUTE, Integer.parseInt(timeHmSplit[1]) + 1);

            heroInfo.workStatus = "Вы работаете с " + timeHm;
            heroInfo.isWorking = true;
            heroInfo.endShift = endShift;
        } else if (matcher2.find()) {
            String minutesToEndJob = matcher2.group(1);
            endShift.add(Calendar.MINUTE, Integer.parseInt(minutesToEndJob) + 1);

            heroInfo.workStatus = "Вы сможете устроится на работу через " + matcher2.group(1) + " мин.";
            heroInfo.isWorking = true;
            heroInfo.endShift = endShift;
        } else {
            HeroInfo defaultHeroInfo = new HeroInfo();
            heroInfo.workStatus = defaultHeroInfo.workStatus;
            heroInfo.isWorking = defaultHeroInfo.isWorking;
            heroInfo.endShift = defaultHeroInfo.endShift;
        }
    }

    private String getCompanyId(String mapPage) throws IOException {
        String REG_HREF_LIST_COMPANY = "\\[<a\\shref='(.+?)'>(.+?)</a>\\]";
        Company bestCompany = getBestCompanyInPage(mapPage, new Company(null, 0));
        Matcher linksToListCompany = Pattern.compile(REG_HREF_LIST_COMPANY).matcher(mapPage);

        while (linksToListCompany.find()) {
            String linkCompanies = linksToListCompany.group(1).replaceAll("&amp;", "&");
            String groupCompanies = linksToListCompany.group(2);
            if (groupCompanies.equals("Добыча") || groupCompanies.equals("Обработка") || groupCompanies.equals("Производство")) {
                bestCompany = getBestCompanyInPage(hwmRequests.getPage(linkCompanies).responseText, bestCompany);
            }
        }

        return bestCompany.id;
    }

    private Company getBestCompanyInPage(String mapPage, Company company) {
        Pattern pattern = Pattern.compile(REG_ID_COMPANY);
        Matcher matcher = pattern.matcher(mapPage);

        while (matcher.find()) {
            int currentSalary = Integer.parseInt(matcher.group(2));
            if (currentSalary >= company.salary) {
                company.id = matcher.group(1);
                company.salary = currentSalary;
            }
        }

        return company;
    }

    private List<AnimalHunting> getAnimalsHunting(String mapPage) {
        List<AnimalHunting> animalsHunting = new ArrayList();
        Matcher matcherAnimals = Pattern.compile(REG_ANIMAL_HUNTING).matcher(mapPage);

        while (matcherAnimals.find()) {
            String nameAnimal = matcherAnimals.group(1);
            int number = Integer.parseInt(matcherAnimals.group(2));
            int gold = Integer.parseInt(matcherAnimals.group(3));

            animalsHunting.add(new AnimalHunting(nameAnimal, number, gold));
        }

        return animalsHunting;
    }

    private void updateInformationFromCompanyPage(String companyPage) throws HWMException, IOException {
        Matcher matcherWorkCode = Pattern.compile(REG_CAPTCHA).matcher(companyPage);
        Matcher matcherReCaptcha = Pattern.compile(REG_RE_CAPTCHA).matcher(companyPage);
        Matcher matcherFineWorkaholic = Pattern.compile(REG_FINE_WORKAHOLIC).matcher(companyPage);

        HeroInfo defaultHeroInfo = new HeroInfo();
        heroInfo.urlToCaptcha = defaultHeroInfo.urlToCaptcha;
        heroInfo.keyReCaptcha = defaultHeroInfo.keyReCaptcha;
        heroInfo.fineWorkaholic = defaultHeroInfo.fineWorkaholic;
        captchaRequestHour = new GregorianCalendar().get(Calendar.HOUR_OF_DAY);

        if (!heroInfo.isWorking) {
            if (matcherWorkCode.find()) {
                heroInfo.urlToCaptcha = HWMRequests.PATH + matcherWorkCode.group();
            } else if (matcherReCaptcha.find()) {
                heroInfo.keyReCaptcha = matcherReCaptcha.group(1);
            }

            if (heroInfo.urlToCaptcha == null && heroInfo.keyReCaptcha == null) {
                if (companyPage.indexOf(TEXT_NOT_RESOURCES) != -1 || companyPage.indexOf(TEXT_NOT_FREE_SLOT) != -1) {
                    goToMap();
                    goToCompany();
                } else {
                    Log.append("sdcard/error:" + new GregorianCalendar().getTime() +".txt", companyPage);
                    throw new HWMException(HWMException.NOT_CAPTCHA);
                }
            } else if (!isConformsStandards(heroInfo.keyReCaptcha != null)) {
                throw new HWMException(HWMException.CODE_DIFFERENT_FROM_STANDARDS);
            }
        }

        if (matcherFineWorkaholic.find()) {
            heroInfo.fineWorkaholic = Double.parseDouble(matcherFineWorkaholic.group(1));
        }
    }

    // Сравнивает страницу с эталонами, смотрит что бы капча не была скрыта
    private boolean isConformsStandards(boolean withReCaptcha) {
        boolean isConformsStandards = false;
        boolean dependenciesIsConformsStandards = true;
        
        Matcher matcherHead = Pattern.compile(REG_HEAD).matcher(companyPage);
        Matcher matcherFrom = Pattern.compile(REG_WORK_FORM).matcher(companyPage);

        for (Map.Entry<String, String> item : standardCodes.dependence.entrySet()) {
            String urlPath = item.getKey();
            String standardCssCode = item.getValue();

            try {
                String currentCssCode = Request.readResult(Request.GET(urlPath));
                if (!standardCssCode.equals(currentCssCode)) {
                    dependenciesIsConformsStandards = false;
                    break;
                }
            } catch (Exception ex) {
                dependenciesIsConformsStandards = false;
            }
        }

        if (matcherHead.find() && matcherFrom.find()) {
            String standardHead = standardCodes.headCompany;
            String standardForm = withReCaptcha ? standardCodes.formReCaptcha : standardCodes.formCaptcha;
            String currentHead = matcherHead.group();
            String currentForm = matcherFrom.group();

            currentHead = currentHead.replaceAll(REG_RE_CAPTCHA_SCRIPT, "");
            currentForm = currentForm.replaceAll(REG_HIDDEN_INPUT, "");

            if (withReCaptcha) {
                currentForm = currentForm.replaceAll(REG_RE_CAPTCHA, "");
            } else {
                currentForm = currentForm.replaceAll(REG_CAPTCHA, "");
                currentForm = currentForm.replaceAll(REG_NUM_EXPRESSION, "");
            }

            if (standardHead == null || standardHead.equals(currentHead) &&
                    (standardForm == null || standardForm.equals(currentForm))) {

                isConformsStandards = true;
            }
        }

        return isConformsStandards && dependenciesIsConformsStandards;
    }
}

class Company {
    public String id;
    public int salary;

    Company(String id, int salary) {
        this.id = id;
        this.salary = salary;
    }
}
