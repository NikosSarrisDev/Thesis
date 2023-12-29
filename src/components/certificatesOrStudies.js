import React from "react";
import {connect} from "react-redux";
import {Button, Card, Container, Typography} from "@material-ui/core";
import { userManager, userManagerOlympus } from "../utils/userManager";

class CertificatesOrStudies extends React.Component {
    render() {
        const { user } = this.props;

        console.log(user)


        return (
            <Container style={styles.root}>
                <Container style={styles.title}>
                    <Typography variant="h5" align="center">
                        Online Course Management System
                        <div style={{padding:"40px", marginTop:"30px", }}>
                            <h6 style={{color: "blue"}}>1.To access the page with the available books of your semester press the button below</h6>
                            <Button size="medium" variant="outlined" style={{color: "black"}}

                                    onClick={(event) => {
                                        event.preventDefault();
                                        // userManager.getUser();
                                        // userManagerOlympus.storeUser();

                                        window.open("http://localhost:8080/applicationform", "_blank");

                                        //this.props.dispatch(push("/login"));
                                    }}
                            >
                                {/*<img src={"/university.png"} width="250" height="160" />*/}
                                <img src={"/exam.png"} width="80" height="80"/>
                            </Button>

                            <br></br>

                            <h6 style={{color: "blue"}}>2.To access the page with the available books of your semester press the button below</h6>
                            <Button size="medium" variant="outlined" style={{color: "black"}}
                                    onClick={(event) => {
                                        event.preventDefault();
                                        // userManager.getUser();
                                        // userManagerOlympus.storeUser();

                                        console.log(user.profile.given_name);
                                        if(Number(user.profile.given_name) === 1){
                                            window.open("http://localhost:8080/BookForm", "_blank");}
                                        if(Number(user.profile.given_name) > 4){
                                            window.open("http://localhost:8080/BookForm4", "_blank");}
                                        window.open("http://localhost:8080/BookForm" + user.profile.given_name, "_blank")



                                    }}>
                                <img src={"/books-stack-of-three.png"} width="80" height="80"/>
                            </Button>
                            <br></br>
                            <Button
                                size="large"
                                onClick={(event) => {
                                    event.preventDefault();
                                    userManager.getUser();
                                    userManagerOlympus.storeUser();
                                    window.open("http://localhost:3000/success", "_self");
                                }}
                                style={{ backgroundColor: '#c91313', color: 'black' }}
                            >
                                Exit Application
                            </Button>
                        </div>
                    </Typography>
                </Container>
            </Container>
        );
    }
}

const styles = {
    root: {
        display: "flex",
        flexDirection: "column",
    },
    buttonBox1: {

        marginTop: "auto",
        marginButtom: "auto",
        width: "auto",
        top: "auto",
        left: "auto",
        transform: 'translate(-50%, -50%)',
        padding: '10px',
    },
    buttonBox2: {

        marginTop: "auto",
        marginButtom: "auto",
        width: "auto",
        top: "auto",
        left: "auto",
        transform: 'translate(-50%, -50%)',
        padding: '10px'
    },

    subtitle: {
        paddingTop: "10px",
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
    centerDiv: {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
    }
};

function mapStateToProps(state) {
    return {
        user: state.oidc.user,
    };
}

export default connect(mapStateToProps)(CertificatesOrStudies);
