package oidc.controller;

import eu.olympus.client.interfaces.UserClient;
import eu.olympus.model.Attribute;
import eu.olympus.model.AttributeIdentityProof;
import eu.olympus.model.Operation;
import eu.olympus.model.Policy;
import eu.olympus.model.Predicate;
import eu.olympus.model.exceptions.AuthenticationFailedException;
import eu.olympus.model.exceptions.ExistingUserException;
import eu.olympus.model.exceptions.OperationFailedException;
import eu.olympus.model.exceptions.TokenGenerationException;
import eu.olympus.model.exceptions.UserCreationFailedException;
import eu.olympus.model.server.rest.IdentityProof;

import java.io.Console;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.jws.WebParam;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import oidc.model.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class OidcController {

    private static final Logger logger = LoggerFactory.getLogger(OidcController.class);

    @Autowired
    UserClient userClient;

    @Autowired
    Policy policy;

    @Autowired
    Storage storage;

    // Login
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(Model model, @RequestParam String redirect_uri, @RequestParam String state, @RequestParam String nonce, HttpServletRequest request) {
        request.getSession().setAttribute("redirectUrl", redirect_uri);
        request.getSession().setAttribute("state", state);
        request.getSession().setAttribute("nonce", nonce);
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        policy.setPolicyId(nonce);
        return "/login";
    }

    @RequestMapping(value = "/loginFailed", method = RequestMethod.GET)
    public String login(Model model) {
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        model.addAttribute("loginError", true);
        return "/login";
    }

    @RequestMapping(value = "/loginPage", method = RequestMethod.GET)
    public String loginPage(Model model) {
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        model.addAttribute("hasCreated", false);
        return "/login";
    }

    @PostMapping("/authenticate")
    public RedirectView authenticate(LoginRequest loginRequest, Model model, HttpServletRequest request) throws AuthenticationFailedException, TokenGenerationException {
        try {
            policy.getPredicates().add(new Predicate("audience", Operation.REVEAL, new Attribute("olympus-service-provider")));
            System.out.println("Predicates: " + policy.getPredicates());
            String token = userClient.authenticate(loginRequest.getUsername(), loginRequest.getPassword(), policy, null, "NONE");
            model.addAttribute("username", loginRequest.getUsername());
            model.addAttribute("token", token);
            logger.info("Policy predicates: {}", policy.getPredicates());

            String redirectUrl = (String) request.getSession().getAttribute("redirectUrl");
            String state = (String) request.getSession().getAttribute("state");
            return new RedirectView(redirectUrl + "#state=" + state + "&id_token=" + token + "&token_type=bearer");
        } catch (Exception e) {
            if (ExceptionUtils.indexOfThrowable(e, AuthenticationFailedException.class) != -1) {
                return new RedirectView("/loginFailed", true);
            } else {
                throw e;
            }
        } finally {
            userClient.clearSession();
        }
    }

    // Logout

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout(Model model, HttpServletRequest request) throws ServletException {
        userClient.clearSession();
        request.getSession().removeAttribute("name");
        request.getSession().removeAttribute("birthdate");
        request.getSession().removeAttribute("given_name");
        request.getSession().removeAttribute("nickname");
        request.getSession().removeAttribute("middle_name");
        request.getSession().removeAttribute("family_name");
        request.getSession().setAttribute("loggedIn", false);
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        model.addAttribute("hasCreated", false);
        return "/login";
    }

    @GetMapping("/admin")
    public String adminPanel(Model model){
        model.addAttribute("adminValid", new AdminValid());
        model.addAttribute("error", false);
        return "administratorValidation";
    }

    @PostMapping("/admin")
    public String adminValid(AdminValid adminValid, Model model){

        if("admin".equals(adminValid.getName()) && "1234".equals(adminValid.getPassword())){
            model.addAttribute("username", adminValid.getName());
            model.addAttribute("password", adminValid.getPassword());
            return "redirect:/createUser";
        }else {
            model.addAttribute("error", true);
            return "administratorValidation";
        }
    }

    // Create User
    @RequestMapping(value = "/createUser", method = RequestMethod.GET)
    public String createNewUser(Model model) {
        model.addAttribute("userExists", false);
        CreateUserRequest createUserRequest = new CreateUserRequest();
        model.addAttribute("createUserRequest", createUserRequest);
        return "/createUser";
    }

    @RequestMapping(value = "/createUser", method = RequestMethod.POST)
    public String postUser(@Valid @ModelAttribute("createUserRequest")CreateUserRequest createUserRequest, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "/createUser";
        }
        try {
            IdentityProof identityProof = constructIdentityProof(createUserRequest);
            userClient.createUserAndAddAttributes(createUserRequest.getUsername(), createUserRequest.getPassword(), identityProof);
        } catch (Exception exception) {
            if (ExceptionUtils.indexOfThrowable(exception, ExistingUserException.class) != -1) {
                System.out.println(ExceptionUtils.indexOfThrowable(exception, ExistingUserException.class));
                model.addAttribute("userExists", true);
            } else if (ExceptionUtils.indexOfThrowable(exception, AuthenticationFailedException.class) != -1) {
                System.out.println(ExceptionUtils.indexOfThrowable(exception, AuthenticationFailedException.class));
                model.addAttribute("userExists", true);
            } else if (ExceptionUtils.indexOfThrowable(exception, UserCreationFailedException.class) != -1) {
                System.out.println(ExceptionUtils.indexOfThrowable(exception, UserCreationFailedException.class));
                model.addAttribute("userExists", true);
            } else {
                model.addAttribute("unknownError", true);
            }
            logger.warn("Create user failed: " + exception);
            return "/createUser";
        }
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        model.addAttribute("hasCreated", true);
        userClient.clearSession();
        return "/login";
    }

    private AttributeIdentityProof constructIdentityProof(CreateUserRequest createUserRequest) {
        Map<String, Attribute> attributes = new HashMap<>();
        attributes.put("name", new Attribute(createUserRequest.getFirstName() + " " + createUserRequest.getLastName()));
        attributes.put("birthdate", new Attribute(createUserRequest.getBirthdate()));
        //applicationcategory for policy because openid
        attributes.put("family_name", new Attribute(createUserRequest.getFamily_name()));
        //studentid for policy because openid
        attributes.put("nickname", new Attribute(createUserRequest.getNickname()));
        //university for policy because openid
        attributes.put("middle_name", new Attribute(createUserRequest.getMiddle_name()));
        //awardeddegree for policy because openid
        attributes.put("given_name", new Attribute(createUserRequest.getGiven_name()));

        return new AttributeIdentityProof(attributes);
    }

    @GetMapping("/form")
    public String displayData(Model model, HttpSession session) {
        // Retrieve the applicationForm object from the session
        New_application applicationForm = (New_application) session.getAttribute("applicationForm");
        session.setAttribute("applicationForm", applicationForm);

        if (applicationForm != null) {
            // Set the applicationForm object in the model

            model.addAttribute("applicationForm", applicationForm);
            model.addAttribute("protocolid", applicationForm.getProtocolid());
            model.addAttribute("requestcode", applicationForm.getRequestcode());
            model.addAttribute("address", applicationForm.getAddress());
            model.addAttribute("phonenumber", applicationForm.getPhonenumber());
            model.addAttribute("fax", applicationForm.getFax());
            model.addAttribute("email", applicationForm.getEmail());
            model.addAttribute("information", applicationForm.getInformation());
            model.addAttribute("semester", applicationForm.getSemester());
            model.addAttribute("modeofstydies", applicationForm.getModeofstudies());
//

        } else {
            // Handle the case where applicationForm is not found in the session
            // You can redirect to an error page or take appropriate action
            return "/error"; // Replace with your actual error page URL
        }

        // Return the Thymeleaf template for displaying the data
        return "form"; // This corresponds to the template name without the ".html" extension
    }

    @GetMapping("/booksVerifyform")
    public String displayDataofBooks1(Model model, HttpSession session) {
        // Retrieve the applicationForm object from the session

        Book bookForm = (Book) session.getAttribute("bookForm");
        session.setAttribute("bookForm", bookForm);

        if (bookForm != null) {
            // Set the applicationForm object in the model

            model.addAttribute("bookForm", bookForm);
            model.addAttribute("semester1books", bookForm.getSemester1books());

        } else {
            // Handle the case where applicationForm is not found in the session
            // You can redirect to an error page or take appropriate action
            return "/error"; // Replace with your actual error page URL
        }

        // Return the Thymeleaf template for displaying the data
        return "booksVerifyform"; // This corresponds to the template name without the ".html" extension
    }

    @GetMapping("/booksVerifyform2")
    public String displayDataofBooks2(Model model, HttpSession session) {
        // Retrieve the applicationForm object from the session

        Book bookForm2 = (Book) session.getAttribute("bookForm2");
        session.setAttribute("bookForm2", bookForm2);

        if (bookForm2 != null) {
            // Set the applicationForm object in the model

            model.addAttribute("bookForm2", bookForm2);
            model.addAttribute("semester2books", bookForm2.getSemester2books());

        } else {
            // Handle the case where applicationForm is not found in the session
            // You can redirect to an error page or take appropriate action
            return "/error"; // Replace with your actual error page URL
        }

        // Return the Thymeleaf template for displaying the data
        return "booksVerifyform2"; // This corresponds to the template name without the ".html" extension
    }

    @GetMapping("/booksVerifyform3")
    public String displayDataofBooks3(Model model, HttpSession session) {
        // Retrieve the applicationForm object from the session

        Book bookForm3 = (Book) session.getAttribute("bookForm3");
        session.setAttribute("bookForm3", bookForm3);

        if (bookForm3 != null) {
            // Set the applicationForm object in the model

            model.addAttribute("bookForm3", bookForm3);
            model.addAttribute("semester3books", bookForm3.getSemester3books());

        } else {
            // Handle the case where applicationForm is not found in the session
            // You can redirect to an error page or take appropriate action
            return "/error"; // Replace with your actual error page URL
        }

        // Return the Thymeleaf template for displaying the data
        return "booksVerifyform3"; // This corresponds to the template name without the ".html" extension
    }

    @GetMapping("/booksVerifyform4")
    public String displayDataofBooks4(Model model, HttpSession session) {
        // Retrieve the applicationForm object from the session

        Book bookForm4 = (Book) session.getAttribute("bookForm4");
        session.setAttribute("bookForm4", bookForm4);

        if (bookForm4 != null) {
            // Set the applicationForm object in the model

            model.addAttribute("bookForm4", bookForm4);
            model.addAttribute("semester4books", bookForm4.getSemester4books());

        } else {
            // Handle the case where applicationForm is not found in the session
            // You can redirect to an error page or take appropriate action
            return "/error"; // Replace with your actual error page URL
        }

        // Return the Thymeleaf template for displaying the data
        return "booksVerifyform4"; // This corresponds to the template name without the ".html" extension
    }


    @RequestMapping("/changePassword")
    public String changePassword(Model model) {
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        model.addAttribute("changePasswordRequest", changePasswordRequest);
        return "/changePassword";
    }


    @PostMapping("/changePassword")
    public String postChangePassword(@Valid ChangePasswordRequest changePasswordRequest, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "/changePassword";
        }
        try {
            userClient.changePassword(changePasswordRequest.getUsername(), changePasswordRequest.getOldPassword(), changePasswordRequest.getNewPassword(), null, "NONE");
        } catch (Exception exception) {
            if (ExceptionUtils.indexOfThrowable(exception, UserCreationFailedException.class) != -1) {
                model.addAttribute("passwordChangeError", true);
            } else if (ExceptionUtils.indexOfThrowable(exception, AuthenticationFailedException.class) != -1) {
                model.addAttribute("usernameWrongError", true);
            } else {
                model.addAttribute("unknownError", true);
            }
            return "/changePassword";
        }
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        model.addAttribute("hasChangedPassword", true);
        userClient.clearSession();
        return "/login";
    }


    @RequestMapping("/deleteAccount")
    public String deleteAccount(Model model) {
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        return "/deleteAccount";
    }


    @PostMapping("/deleteAccount")
    public String postDeleteAccount(LoginRequest loginRequest, Model model) {
        try {
            userClient.deleteAccount(loginRequest.getUsername(), loginRequest.getPassword(), null, "NONE");
        } catch (Exception exception) {
            if (ExceptionUtils.indexOfThrowable(exception, AuthenticationFailedException.class) != -1) {
                model.addAttribute("userDeletionError", true);
            } else {
                model.addAttribute("unknownError", true);
            }
            return "/deleteAccount";
        }
        loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        model.addAttribute("hasDeletedAccount", true);
        return "/login";
    }

    @RequestMapping(value = "/loginViaCredential", method = RequestMethod.GET)
    public String loginViaCredential(){
        return "/loginViaCredential";
    }


    private String getFrontpage(Model model) {

        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);

        return "/login";
    }

    @GetMapping("/verify")
    public String verify(Model model, HttpServletRequest request) {



        model.addAttribute("username", request.getSession().getAttribute("username"));
        model.addAttribute("policy", request.getSession().getAttribute("policy"));


        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        System.out.println(request.getSession());

        return "/verify";
    }

    @GetMapping("/storage")
    public String hello(Model model, HttpServletRequest request) {
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        model.addAttribute("hasCreated", false);
        return "/storage";
    }

    @PostMapping("/storage")
    public String showUserInfo(Model model, HttpServletRequest request, LoginRequest loginRequest) throws AuthenticationFailedException, OperationFailedException {
        String token = userClient.authenticate(loginRequest.getUsername(), loginRequest.getPassword(), policy, null, "NONE");
        storage.checkCredential();
        System.out.println(storage.checkCredential());

        model.addAttribute("username", loginRequest.getUsername());
        model.addAttribute("token", token);
        model.addAttribute("firstname", request.getSession().getAttribute("firstname"));
        model.addAttribute("studentid", request.getSession().getAttribute("studentid"));
        model.addAttribute("university", request.getSession().getAttribute("university"));
        model.addAttribute("awardeddegree", request.getSession().getAttribute("awardeddegree"));
        model.addAttribute("loginRequest", loginRequest);

        return "storage";
    }



    @RequestMapping("manageAccountLogin")
    public String manageAccountLogin(Model model) {
        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        return "/manageAccountLogin";
    }


    @RequestMapping(value = "/applicationform", method = RequestMethod.GET)
    public String showApplicationForm(Model model, HttpSession session) {
        // Retrieve the ApplicationForm object from the session, if it exists
        New_application applicationForm = (New_application) session.getAttribute("applicationForm");


        if (applicationForm == null) {
            applicationForm = new New_application();
            // Populate the fields with data using setter methods
            applicationForm.setDate("");
            applicationForm.setProtocolid("");
            applicationForm.setRequestcode("");
            applicationForm.setAddress("");
            applicationForm.setPhonenumber("");
            applicationForm.setFax("");
            applicationForm.setEmail("");
            applicationForm.setInformation("");
            applicationForm.setModeofstudies("");


            session.setAttribute("applicationForm", applicationForm);

        }
        System.out.println(applicationForm);
        // Set the applicationForm object as an attribute in the model
        model.addAttribute("applicationForm", applicationForm);
        model.addAttribute("date", applicationForm.getDate());
        model.addAttribute("protocolid", applicationForm.getProtocolid());
        model.addAttribute("requestcode", applicationForm.getRequestcode());
        model.addAttribute("address", applicationForm.getAddress());
        model.addAttribute("phonenumber", applicationForm.getPhonenumber());
        model.addAttribute("fax", applicationForm.getFax());
        model.addAttribute("information", applicationForm.getInformation());
        model.addAttribute("semester", applicationForm.getSemester());
        model.addAttribute("modeofstudies", applicationForm.getModeofstudies());

        // You can set more attributes as needed for the view
        session.setAttribute("applicationForm", applicationForm);

        // Then, return the view name
        return "applicationform"; // Replace with your actual view name
    }

    @PostMapping("/applicationform")
    public String submitApplicationForm(New_application applicationForm, HttpSession session) {


        applicationForm.getDate();
        applicationForm.getProtocolid();
        applicationForm.getRequestcode();
        applicationForm.getAddress();
        applicationForm.getPhonenumber();
        applicationForm.getFax();
        applicationForm.getEmail();
        applicationForm.getInformation();
        applicationForm.getSemester();
        applicationForm.getModeofstudies();

        // Store the updated applicationForm object in the session
        session.setAttribute("applicationForm", applicationForm);



//        System.out.println(applicationForm);
        // Redirect to a success page or perform other actions as needed
        return "redirect:form"; // Replace with the actual success page URL

    }

    @RequestMapping(value = "/BookForm", method = RequestMethod.GET)
    public String showBooksForm(Model model, HttpSession session){

        Book bookForm = (Book) session.getAttribute("bookForm");

        if (bookForm == null) {
            // Create a new Book object if it's not present in the session
            bookForm = new Book();
            session.setAttribute("bookForm", bookForm);
        }

        model.addAttribute("bookForm", bookForm);
        model.addAttribute("semester1books", bookForm);
//        model.addAttribute("semester2books", bookForm);
//        model.addAttribute("semester3books", bookForm);
//        model.addAttribute("semester1books", bookForm);

        session.setAttribute("bookForm", bookForm);

        return "BookForm";
    }

    @PostMapping(value = "/BookForm")
    public String submitBookForm(Book bookForm, HttpSession session){
        bookForm.getSemester1books();
//        bookForm.getSemester2books();
//        bookForm.getSemester3books();
//        bookForm.getSemester4books();

        session.setAttribute("bookForm", bookForm);
        return "redirect:booksVerifyform";
    }

    @RequestMapping(value = "/BookForm2", method = RequestMethod.GET)
    public String showBooksForm2(Model model, HttpSession session){

        Book bookForm2 = (Book) session.getAttribute("bookForm2");

        if (bookForm2 == null) {
            // Create a new Book object if it's not present in the session
            bookForm2 = new Book();
            session.setAttribute("bookForm2", bookForm2);
        }

        model.addAttribute("bookForm2", bookForm2);
//        model.addAttribute("semester1books", bookForm2);
        model.addAttribute("semester2books", bookForm2);
//        model.addAttribute("semester3books", bookForm);
//        model.addAttribute("semester1books", bookForm);

        session.setAttribute("bookForm", bookForm2);

        return "BookForm2";
    }

    @PostMapping(value = "/BookForm2")
    public String submitBookForm2(Book bookForm2, HttpSession session){
        bookForm2.getSemester2books();
//        bookForm.getSemester2books();
//        bookForm.getSemester3books();
//        bookForm.getSemester4books();

        session.setAttribute("bookForm2", bookForm2);
        return "redirect:booksVerifyform2";
    }

    @RequestMapping(value = "/BookForm3", method = RequestMethod.GET)
    public String showBooksForm3(Model model, HttpSession session){

        Book bookForm3 = (Book) session.getAttribute("bookForm3");

        if (bookForm3 == null) {
            // Create a new Book object if it's not present in the session
            bookForm3 = new Book();
            session.setAttribute("bookForm3", bookForm3);
        }

        model.addAttribute("bookForm3", bookForm3);
        model.addAttribute("semester3books", bookForm3);
//        model.addAttribute("semester2books", bookForm);
//        model.addAttribute("semester3books", bookForm);
//        model.addAttribute("semester1books", bookForm);

        session.setAttribute("bookForm3", bookForm3);

        return "BookForm3";
    }

    @PostMapping(value = "/BookForm3")
    public String submitBookForm3(Book bookForm3, HttpSession session){
        bookForm3.getSemester3books();
//        bookForm.getSemester2books();
//        bookForm.getSemester3books();
//        bookForm.getSemester4books();

        session.setAttribute("bookForm3", bookForm3);
        return "redirect:booksVerifyform3";
    }

    @RequestMapping(value = "/BookForm4", method = RequestMethod.GET)
    public String showBooksForm4(Model model, HttpSession session){

        Book bookForm4 = (Book) session.getAttribute("bookForm4");

        if (bookForm4 == null) {
            // Create a new Book object if it's not present in the session
            bookForm4 = new Book();
            session.setAttribute("bookForm4", bookForm4);
        }

        model.addAttribute("bookForm4", bookForm4);
        model.addAttribute("semester4books", bookForm4);
//        model.addAttribute("semester2books", bookForm);
//        model.addAttribute("semester3books", bookForm);
//        model.addAttribute("semester1books", bookForm);

        session.setAttribute("bookForm4", bookForm4);

        return "BookForm4";
    }

    @PostMapping(value = "/BookForm4")
    public String submitBookForm4(Book bookForm4, HttpSession session){
        bookForm4.getSemester4books();
//        bookForm.getSemester2books();
//        bookForm.getSemester3books();
//        bookForm.getSemester4books();

        session.setAttribute("bookForm4", bookForm4);
        return "redirect:booksVerifyform4";
    }


    @RequestMapping(value = "/getpolicy", method = RequestMethod.GET)
    public String showPhpVerificationPage(Model model, HttpSession session) {
        // Retrieve the PHP application form object from the session

        // Retrieve the New application form object from the session
        New_application applicationForm = (New_application) session.getAttribute("applicationForm");


        if (applicationForm == null) {
            // New application form data doesn't exist, so initialize it
            applicationForm = new New_application();
            // Initialize the New application form fields as needed
            // Example:
//            applicationForm.setApplicationentry("");
//            applicationForm.setAcademicprogramme("");
            applicationForm.setDate("");
            applicationForm.setProtocolid("");
            applicationForm.setRequestcode("");
            applicationForm.setAddress("");
            applicationForm.setPhonenumber("");
            applicationForm.setFax("");
            applicationForm.setEmail("");
            applicationForm.setInformation("");

            // Initialize other New application form fields as needed

            // Store the initialized New application form data in the session
            session.setAttribute("applicationForm", applicationForm);
        }

        // Add attributes for New application form
        model.addAttribute("newApplicationForm", applicationForm);
//        model.addAttribute("newApplicationEntry", applicationForm.getApplicationentry());
//        model.addAttribute("newAcademicProgramme", applicationForm.getAcademicprogramme());
        model.addAttribute("newDate", applicationForm.getDate());
        model.addAttribute("newProtocolid", applicationForm.getProtocolid());
        model.addAttribute("newAddress", applicationForm.getAddress());
        model.addAttribute("newPhoneNumber", applicationForm.getPhonenumber());
        model.addAttribute("newFax", applicationForm.getFax());
        model.addAttribute("newEmail", applicationForm.getEmail());
        model.addAttribute("newInformation", applicationForm.getInformation());
        model.addAttribute("newSemester", applicationForm.getSemester());
        model.addAttribute("newModeofStudies", applicationForm.getModeofstudies());
        // Add more attributes for other New application form fields as needed

        // Return the PHP verification page template, which includes both forms
        return "verify"; // Replace with your actual template name
    }

    @RequestMapping(value = "/getpolicyBookForm1", method = RequestMethod.GET)
    public String bookForm1Verify(Model model, HttpSession session){

        Book bookForm1 = (Book) session.getAttribute("bookForm1");

        if(bookForm1 == null){
            bookForm1 = new Book();

//            bookForm1.setSemester1books(new String[]{""});
            session.setAttribute("bookForm1",bookForm1);
        }

        model.addAttribute("newBookForm1", bookForm1);
        model.addAttribute("newsemester1books", bookForm1.getSemester1books());
        return "bookForm1verify";
    }

    @RequestMapping(value = "/getpolicyBookForm2", method = RequestMethod.GET)
    public String bookForm2Verify(Model model, HttpSession session){

        Book bookForm2 = (Book) session.getAttribute("bookForm2");

        if(bookForm2 == null){
            bookForm2 = new Book();

//            bookForm2.setSemester2books(new String[]{""});
            session.setAttribute("bookForm2",bookForm2);
        }

        model.addAttribute("newBookForm2", bookForm2);
        model.addAttribute("newsemester2books", bookForm2.getSemester2books());
        return "bookForm2verify";
    }

    @RequestMapping(value = "/getpolicyBookForm3", method = RequestMethod.GET)
    public String bookForm3Verify(Model model, HttpSession session){

        Book bookForm3 = (Book) session.getAttribute("bookForm3");

        if(bookForm3 == null){
            bookForm3 = new Book();

//            bookForm3.setSemester3books(new String[]{""});
            session.setAttribute("bookForm3",bookForm3);
        }

        model.addAttribute("newBookForm3", bookForm3);
        model.addAttribute("newsemester3books", bookForm3.getSemester3books());
        return "bookForm3verify";
    }

    @RequestMapping(value = "/getpolicyBookForm4", method = RequestMethod.GET)
    public String bookForm4Verify(Model model, HttpSession session){

        Book bookForm4 = (Book) session.getAttribute("bookForm4");

        if(bookForm4 == null){
            bookForm4 = new Book();

//            bookForm4.setSemester4books(new String[]{""});
            session.setAttribute("bookForm4",bookForm4);
        }

        model.addAttribute("newBookForm4", bookForm4);
        model.addAttribute("newsemester4books", bookForm4.getSemester4books());
        return "bookForm4verify";
    }

    @GetMapping("/error")
    public String showErrorPage() {
        return "error"; // This corresponds to the name of your HTML file (without the extension)
    }
}