import React, {useState} from "react";

import {AppBar, Button, Container, IconButton, Link, Menu, MenuItem, Typography , Toolbar, Box} from "@material-ui/core";
import { userManager, userManagerOlympus } from "../utils/userManager";
// import {userManager, userManagerOlympus} from "../../../olympus-service-provider/src/utils/userManager";
import {push} from "react-router-redux";
import swal from "sweetalert";
import {connect} from "react-redux";
import {Card} from "@material-ui/core";

import PopupState, {bindTrigger, bindMenu} from 'material-ui-popup-state';
import 'react-swipe-to-delete-component/dist/swipe-to-delete.css';
import SwipeToDelete from 'react-swipe-to-delete-component';

class StorageC extends React.Component{
    render() {
        const {user} = this.props;

        console.log(user)

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
                                                userManager.getUser();
                                                userManagerOlympus.storeUser();
                                                sessionStorage.clear();
                                                window.close();
                                                window.open("http://localhost:8080/logout", "_blank");}}style={{color:"red"}}>Logout</MenuItem>
                                        </Menu>

                                    </React.Fragment>
                                )}
                            </PopupState>

                        </Toolbar>
                    </AppBar>

                    <Typography variant="subtitle1" align="center">
                        <img src={"/university.png"} width="250" height="160"/>

                        <Typography variant="body1" align="center">


                            <SwipeToDelete

                                axis="x"


                                onTouchMove={(touchMoveEvent) => console.log(touchMoveEvent)}

                                onDrag={this.handleEvent}
                                onStart={this.handleEvent}
                                onStop={this.handleEvent}
                                onMouseDown={this.handleEvent}
                                onMouseUp={this.handleEvent}
                                onTouchStart={this.handleEvent}
                                onTouchEnd={this.handleEvent}>

                                <Card>
                                    <details>
                                        <summary>Credentials Storage</summary>
                                        <h6 style={{color: "black"}}>FullName:{user ? user.profile.name : "Mister Unknown"}  </h6>
                                        <h6 style={{color: "black"}}>Birthdate: {user ? user.profile.birthdate : ""}</h6>
                                        <h6 style={{color: "black"}}>Years of Studies: {user ? user.profile.given_name : "University"}</h6>
                                        <h6 style={{color: "black"}}>AM: {user ? user.profile.nickname : "-"}</h6>
                                        <h6 style={{color: "black"}}>Address: {user ? user.profile.middle_name : "-"}</h6>
                                        <h6 style={{color: "black"}}>Phone Number: {user ? user.profile.family_name : "-"}</h6>

                                    </details>

                                </Card>

                            </SwipeToDelete>



                        </Typography>


                    </Typography>

                </Container>
            </Container>
        )

}
}

const styles = {
    root: {
        display: "flex",
        flexDirection: "column",
    },
    title: {
        flex: "1 0 auto",
    },
    list: {
        listStyle: "none",
    },
    li: {
        display: "flex",
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

export default connect(mapStateToProps)(StorageC);
