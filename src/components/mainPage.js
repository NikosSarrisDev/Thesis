import React from "react";
import { connect } from "react-redux";
import { userManager, userManagerOlympus } from "../utils/userManager";
import {Button, Container, Typography, MenuItem, Menu, IconButton, AppBar} from "@material-ui/core";
import Box from "@material-ui/core/Box";
import Toolbar from "@material-ui/core/Toolbar";
import {push} from "react-router-redux";
import LoginPage from "./loginPage";
import PopupState, { bindTrigger, bindMenu } from 'material-ui-popup-state';
import swal from 'sweetalert';

class MainPage extends React.Component {

  componentDidMount() {
    // Perform data verification when the component mounts
    this.verifyUserData(this.props.user);
  }


  // Function to verify user data
  verifyUserData(user) {

    const requiredFields = ["name", "sub", "birthdate", "given_name", "nickname", "middle_name", "family_name"];

    if (user && user.profile) {
      // Check if all required fields exist in the user object

      const missingFields = requiredFields.filter((field) => !user.profile[field]);

      if (missingFields.length === 0) {

        // All required fields are present, data is valid

      } else if(missingFields.length >=1) {
        // Some required fields are missing
        swal({
          title: "Error",
          text: `Missing fields: ${missingFields.join(", ")}`,
          icon: "error",
          button: "Close",
        });
      }
      else {
        swal({
          title:"Error",
          text: `User, ${user.profile.name} already posses a valid credential`,
          icon: "error",
          confirmButtonText: "Return to login page and sign in via credential"
        }).then((result) => {
          if(result.isConfirmed){
            userManager.getUser();
            userManagerOlympus.storeUser();
            window.close()
            window.open("http://localhost:8080/logout")
          }
        })
      }
    } else {
      // User object or profile data is missing
      swal({
        title: "Error",
        text: "User data is missing or incomplete.",
        icon: "error",
        button: "Close",
      });
    }
  }



  render() {
    const { user } = this.props;
    console.log("start user");
    console.log(user);
    console.log("end user");

    // let birthdate = user.profile.birthdate;
    let birthdate = 24;
    let today = new Date();
    console.log(today.getYear());
    today.setYear(today.getYear() + 1900 - 18);
    let over18 = new Date(birthdate) < today;

    if (!over18) {
      alert(" Your age is under 18, sending back to the login page...");
      return <LoginPage />;
    }
    // sessionStorage.setItem("yearofstudy", user.profile.given_name);

    return (
        <Container style={styles.root}>
          <Container style={styles.title}>
            <Box sx={{ flexGrow: 1 }}>

            </Box>
            <AppBar position="static">
              {/*style={{position: 'fixed',*/}
              {/*top: 0,*/}
              {/*right: 0,*/}
              {/*padding: '10px'}}*/}
              <Toolbar>
                <IconButton
                    size="large"
                    edge="start"
                    color="inherit"
                    aria-label="menu"
                    sx={{ mr: 2 }}
                >
                  {/*<MenuIcon />*/}
                  <img src={"/user.png"} alt="User" width="40" height="40"/>
                </IconButton>
                <PopupState variant="popover" popupId="demo-popup-menu" >
                  {(popupState) => (
                      <React.Fragment>
                        <Button color="inherit" uppercase={false} {...bindTrigger(popupState)}
                                size="medium"


                        >
                          {user ? user.profile.name : "Mister Unknown"}
                        </Button>

                        <Menu {...bindMenu(popupState)}>
                          {/*<MenuItem onClick={()=>{}}style={{color: "red"}}>Welcome {user ? user.profile.name : "Mister Unknown"  }</MenuItem>*/}
                          <MenuItem onClick={(event)=>{
                            event.preventDefault();
                            userManager.getUser();
                            userManagerOlympus.storeUser().then(r => this.props.dispatch(push("/success")))}}>Home</MenuItem>
                          <MenuItem onClick={(event)=>{
                            event.preventDefault();
                            userManager.getUser();
                            userManagerOlympus.storeUser()
                            swal({
                              title: "Privacy notice!",
                              text: "Sensitive informations will be render to the screen",
                              icon: "warning",
                              button: "Ok",
                            }).then(r => this.props.dispatch(push("/information")))}}>Personal Information</MenuItem>
                          <MenuItem onClick={(event)=>{
                            event.preventDefault();
                            userManager.getUser();
                            userManagerOlympus.storeUser();
                            swal({
                              title: "Redirection!",
                              text: "Moving to credential Storage",
                              icon: "warning",
                              button: "Ok",
                            }).then(r => this.props.dispatch(push("/Storage")))}}>View your possesed Credentials</MenuItem>
                          <MenuItem onClick={(event)=>{
                            event.preventDefault();
                            userManager.getUser();
                            userManagerOlympus.storeUser();
                            window.close();
                            window.open("http://localhost:8080/changePassword", "_blank");}}>Change Password</MenuItem>
                          <MenuItem onClick={(event)=>{
                            event.preventDefault();
                            userManager.getUser();
                            userManagerOlympus.storeUser();
                            window.close();
                            window.open("http://localhost:8080/deleteAccount", "_blank");}}>Delete Account</MenuItem>

                          <MenuItem onClick={(event)=>{
                            event.preventDefault();
                            userManager.removeUser()
                            userManagerOlympus.removeUser();
                            sessionStorage.clear();
                            window.close();
                            window.open("http://localhost:8080/logout", "_blank");}}style={{color:"red"}}>Logout</MenuItem>
                        </Menu>

                      </React.Fragment>
                  )}
                </PopupState>

              </Toolbar>
            </AppBar>

          </Container>
          <h3>Welcome, student {user ? user.profile.name : "Mister Unknown"}</h3>

          {/*<h5 style={{color: "blue"}}>To get a credential for your curriculum choose the option"Get Credential for PESTO Client"</h5>*/}
          {/*<Button*/}
          {/*    size="medium"*/}
          {/*    variant="outlined"*/}
          {/*    onClick={(event) => {*/}
          {/*        event.preventDefault();*/}
          {/*        // userManager.getpolicy()*/}
          {/*        //policy edw kai meta store*/}
          {/*        userManager.getUser();*/}
          {/*        userManagerOlympus.storeUser().then(r => this.props.dispatch(push("/create/credential"))*/}
          {/*);*/}
          {/*        //window.open("http://localhost:8080/applicationform", "_blank");*/}

          {/*    }}*/}
          {/*>*/}
          {/*    Get Credential for PESTO Client*/}
          {/*</Button>*/}


          <h5 style={{color: "blue"}}>Access the Online Course Management System</h5>

          <Button
              size="medium"
              variant="outlined"
              onClick={(event) => {
                event.preventDefault();
                // userManager.getUser();
                // userManagerOlympus.storeUser()
                // userManagerOlympus.signinRedirect();
                window.close();
                window.open("http://localhost:8080/loginViaCredential", "_blank");

              }}
          >
            Online Course Management System
          </Button>


        </Container>
    );
  }
}

const styles = {
  root: {
    display: "flex",
    flexDirection: "column"
  },
  title: {
    flex: "1 0 auto"
  },
  list: {
    listStyle: "none"
  },
  li: {
    display: "flex"
  },
  buttonBox: {
    paddingTop: "10px",
    marginTop: "auto",
    marginButtom: "auto",
    width: "150px",
  },
  subtitle: {
    paddingTop: "10px",
  },
};

function mapStateToProps(state) {
  return {
    user: state.oidc.user,
  };
}

export default connect(mapStateToProps)(MainPage);

