import React from "react";
import { Router, Route, browserHistory } from "react-router";
import HomePage from "../components/homePage";
import { syncHistoryWithStore } from "react-router-redux";
import CallbackPageOL from "../components/callbackOL";
import CallbackPageKC from "../components/callbackKC";
import LoginPage from "../components/loginPage";
import MainPage from "../components/mainPage";
import store from "../store/store";
import CredentialStorage from "../components/credentialStorage";
import CheckalreadyCredential from "../components/checkalreadyCredential";
import CertificatesOrStudies from "../components/certificatesOrStudies";
import personalization from "../components/personalization";
import StorageC from "../components/Storage";

const history = syncHistoryWithStore(browserHistory, store);

export default function Routes(props) {
  return (
    <Router history={history}>
      <Route path="/" component={HomePage} /> 
      <Route path="/login" component={LoginPage} />
      <Route path="/success" component={MainPage} />
      <Route path="/callbackOL" component={CallbackPageOL} />
      <Route path="/callbackKC" component={CallbackPageKC} />
      <Route path="/create/credential" component={CredentialStorage}/>
      <Route path="/already/credential" component={CheckalreadyCredential} />
      <Route path="/applicationForm" component={CertificatesOrStudies} />
      <Route path="/information" component={personalization}/>
      <Route path="/Storage" component={StorageC} />
    </Router>
  );
}
