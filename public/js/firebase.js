// public/js/firebase.js
import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";

const firebaseConfig = {
  apiKey: "AIzaSyBHB8Ci6fu3dErYadSKVGp5Jy7rhn58m7I",
  authDomain: "studyplanner-a3e84.firebaseapp.com",
  projectId: "studyplanner-a3e84",
  storageBucket: "studyplanner-a3e84.appspot.com",
  messagingSenderId: "444326131386",
  appId: "1:444326131386:web:766305f4d29ec26283f483",
  measurementId: "G-K1T9Q31FJZ"
};

const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);

export { app, analytics };
