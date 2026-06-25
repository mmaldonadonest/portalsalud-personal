/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Miguel
 */
public class Test {
    public static void main(String[] args){
        System.out.println(""+new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("admin"));
    }
}
