//
//  ViewController.swift
//  eRehabr
//
//  Created by Devan Carlson on 8/15/16.
//  Copyright © 2016 Devan Carlson. All rights reserved.
//

import UIKit
import FBSDKLoginKit

class LoginViewController: UIViewController, UITextFieldDelegate, FBSDKLoginButtonDelegate {
    
    var strEmail, strPass: String?
    
    @IBOutlet weak var FacebookButton: FBSDKLoginButton!
    @IBOutlet weak var PasswordTextField: UITextField!
    @IBOutlet weak var EmailTextField: UITextField!
    
    struct UserKeys {
        var AccountID = "AccountID"
        var Name = "Name"
        var FIRToken = "FIRToken"
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        EmailTextField.delegate = self
        PasswordTextField.delegate = self
        
        if strEmail != nil && strPass != nil {
            EmailTextField.text = strEmail
            PasswordTextField.text = strPass
        }
        
        FacebookButton.readPermissions = ["public_profile", "email"]
        FacebookButton.delegate = self
    }
    
    override func viewDidAppear(_ animated: Bool) {
        loadUserDefaults()
    }
    
    func loginButton(_ loginButton: FBSDKLoginButton!, didCompleteWith result: FBSDKLoginManagerLoginResult!, error: NSError!){
        print("logged in with facebook")
        FBSDKGraphRequest.init(graphPath: "me", parameters: ["fields":"first_name, last_name, email"]).start { (connection, result, error) -> Void in
            let strFirstName: String = (result.object(forKey: "first_name") as? String)!
            let strLastName: String = (result.object(forKey: "last_name") as? String)!
            let strEmail = (result.object(forKey: "email") as? String)!
            let id = (result.object(forKey: "id") as? String)!
            let name = strFirstName + " " + strLastName
            
            postForMap(["Name":name, "Email":strEmail, "Password":"", "FacebookID":id], urlExtension: "/facebookLogin", callbackSuccess: self.facebookLoginSuccess)
        }
    }
    
    func facebookLoginSuccess(_ resp: [String:AnyObject], statusCode: Int) {
        loginSuccess(resp, statusCode: statusCode)
    }
    
    func loginButtonDidLogOut(_ loginButton: FBSDKLoginButton!){
        print("logged out of facebook")
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    func loadUserDefaults() {
        
        if Common.getAccountID() != nil {
            print("Found Account id = " + Common.getAccountID()!)
            self.performSegue(withIdentifier: "LoginPressedSegue", sender: self)
        } else {
            let loginManager = FBSDKLoginManager()
            loginManager.logOut()
        }
    }
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        if (textField === EmailTextField) {
            PasswordTextField.becomeFirstResponder()
        } else if (textField === PasswordTextField) {
            PasswordTextField.resignFirstResponder()
            loginPressed()
        }
        
        return true
    }

    @IBAction func loginPressed() {
        print("loginPressed")
        EmailTextField.resignFirstResponder()
        PasswordTextField.resignFirstResponder()
        postForMap(["Email":EmailTextField.text!, "Password":PasswordTextField.text!], urlExtension: "/login", callbackSuccess: loginSuccess)
    }
    
    func loginSuccess(_ responseJSON: [String:AnyObject], statusCode: Int) {
        print("login success")
        let userDefaults = UserDefaults.standard
        
        responseJSON.forEach({(key: String, value: AnyObject) in
            if key == "Response" {
                if let msg = value as? String {
                    let alert = UIAlertController(title: "Error", message: msg, preferredStyle: UIAlertControllerStyle.alert)
                    alert.addAction(UIAlertAction(title: "Back", style: UIAlertActionStyle.default, handler: nil))
                    self.present(alert, animated: true, completion: nil)
                    return
                }
            }
            if key == "Name" {
                let name = value as? String
                userDefaults.set(name!, forKey: UserKeys().Name)
            }
            if key == "AccountID" {
                print("setting userdefaults accountID")
                let accountID = value as? String
                userDefaults.set(accountID!, forKey: UserKeys().AccountID)
            }
        })
        userDefaults.synchronize()
        DispatchQueue.main.async(execute: {
            self.performSegue(withIdentifier: "LoginPressedSegue", sender: self)
        })
    }
    
    @IBAction func viewTapped() {
        print("view tapped")
        EmailTextField.resignFirstResponder()
        PasswordTextField.resignFirstResponder()
    }
}

