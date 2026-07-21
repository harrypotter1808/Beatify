package com.Main;
import com.Admin.*; 
import com.User.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Scanner;


public class MainSpotifyEntry {
	static Scanner sc=new Scanner(System.in);
	static Admin admin=new Admin();
	static User user=new User();
	static ArrayList<Song> globallibrary=new ArrayList<Song>();
	
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException, InterruptedException {
		System.out.println("---WELCOME TO SPOTIFY---");
		System.out.println("1.Admin");
		System.out.println("2.Login");
		System.out.println("---------------");
		int ch=sc.nextInt();
		switch(ch) {
		case 1->LoginAdmin();
		case 2->LoginUser();
		}
	}


	private static void LoginUser() throws ClassNotFoundException, SQLException, InterruptedException {
		boolean flag=true;
		while(true) {
		System.out.println("1.Create new Account");
		System.out.println("2.Login");
		System.out.println("3.exit");
		int ch=sc.nextInt();
		switch(ch) {
		case 1->{
			Connection connection= dbConnect();
		PreparedStatement statement = connection.prepareStatement("insert into user values(?,?,?,?,?)");
		System.out.println("Enter name:");
		statement.setString(1, sc.next());
		System.out.println("Enter email");
		statement.setString(2, sc.next());
		System.out.println("Enter password");
		statement.setString(3, sc.next());
        System.out.println("Enter mobile no");
        statement.setString(4, sc.next());
        System.out.println("Enter your location:");
        statement.setString(5, sc.next());

        int result=statement.executeUpdate();
        System.out.println(result==1?"Account created successfully":"Account not created");
        connection.close();
        statement.close();
		}
		case 2->{
			Connection connection=dbConnect();
		PreparedStatement statement=	connection.prepareStatement("select * from user where username=? and password=?");
		System.out.println("Enter username");
		String username=sc.next();
		statement.setString(1, username);
		System.out.println("Enter pass:");
		statement.setString(2, sc.next());
		
		ResultSet set=statement.executeQuery();
		boolean flag1=true;
		while(set.next()) {
			System.out.println("User Found!");
			System.out.println("-----User Menu-------");
			System.out.println("1.Play Song");
			System.out.println("2.Create A playlist");
			System.out.println("3,Add songs from palylist");
			System.out.println("4.Remove song playlist");
			System.out.println("5.Show All songs");
			
			System.out.println("----------------------");
			System.out.println("Enter your choice");
			int n=sc.nextInt();
			switch(n) {
			case 1->{
				ShowSongs();
				System.out.println("Enter song name:");
				String song=sc.next();
				System.out.println("Playing a song ("+song+")....");
				Thread.sleep(5000);
				
			}
			case 2->{
				PreparedStatement statement2=connection.prepareStatement("insert into playlist values(?,?,?)");
				System.out.println("Enter a playlist name");
				statement2.setString(1, sc.next());
				statement2.setString(2, username);
				ShowSongs();
				System.out.println("Enter song name:");
				statement2.setString(3, sc.next());
				
				int res=statement2.executeUpdate();
				System.out.println(res==1?"playlist created":"playlist not created");
			}
			case 3->{
				PreparedStatement statement2=connection.prepareStatement("insert into playlist values(?,?,?)");
				System.out.println("Enter a playlist name");
				statement2.setString(1, sc.next());
				statement2.setString(2, username);
				ShowSongs();
				System.out.println("Enter song name:");
				statement2.setString(3, sc.next());
				
				int res=statement2.executeUpdate();
				System.out.println(res==1?"song added":"song not added");

			}
			case 4->{
				
			}
			case 5->{
				ShowSongs();
			}
		case 6->{
			flag1=false;
		}
			
			}
			
			return;
		}
		}
		}
	}
	}


	private static void LoginAdmin() throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.cj.jdbc.Driver");
		System.out.println("Enter username:");
		String user=sc.next();
		System.out.println("Enter password:");
		String pass=sc.next();
		if(admin.isAuthenticate(user,pass)) {
			boolean flag=true;
			while(flag) {
			System.out.println("-----Admin menu------");
			System.out.println("1.Add song");
			System.out.println("2.update song details");
			System.out.println("3.delete a song");
			System.out.println("4.show all song details ");
			System.out.println("5.exit");
			System.out.println("enter choice:");
			int ch=sc.nextInt();
			switch(ch) {
			case 1->{
				System.out.println("Enter a song title:");
				String title=sc.next();
				System.out.println("enter song artist");
				String artist=sc.next();
				System.out.println("Enter song duration");
				double duration=sc.nextDouble();
				System.out.println("Enter song genre");
				String genre=sc.next();
				Song song=new Song(title, artist, duration, genre);
				globallibrary.add(song);
				Connection Connection=dbConnect();
			    PreparedStatement Statement=Connection.prepareStatement("insert into song values(?,?,?,?)");
				Statement.setString(1,title);
				Statement.setString(2,artist);
				Statement.setDouble(3,duration);
		     	Statement.setString(4,genre);
				
				Statement.executeUpdate();
				Connection.close();
				Statement.close();
				System.out.println("song added");
			}
      		case 2-> {
      			System.out.println("enter song title:");
      			String title=sc.next();
      			System.out.println("1.artist");
      			System.out.println("2.duration");
      			System.out.println("3.genre");
      			
      			int ch1=sc.nextInt();
      			String query="";
      			
      			switch(ch1) {
      			case 1->{
      				System.out.println("Enter new artist name:");
      				query="update song set artist='"+sc.next()+"'where title=?";
      			}
      			case 2->{
      				System.out.println("Enter new duration");
      				query="update song set duration='"+sc.nextDouble()+"'where title=?";
      			}
      			case 3->{
      				System.out.println("Enter new genre:");
      				query="update song set genere='"+sc.next()+"'where title=?";
      			}
      			}
      		     Connection connection= dbConnect();
      			 PreparedStatement statement= connection.prepareStatement(query);
      			 statement.setString(1, title);
      			 int result=statement.executeUpdate();
      			 if(result==1) {
      				 System.out.println("song details updated");
      			 }else {
      				 System.out.println("song does not found");
      			 }
      			}
      		
      		case 3->{
      			System.out.println("enter song title");
      			String tittle=sc.next();
      		Connection connection= dbConnect();
      		PreparedStatement statement=connection.prepareStatement("delete from song where title=?");
      		statement.setString(1, tittle);
      		int result=statement.executeUpdate();
      		System.out.println(result==1?" song deleted":"song not deleted");
      		}
      		
      		case 4->{
      			System.out.println("---song List---");
         	      Connection connection=dbConnect();
         	    PreparedStatement Statement= connection.prepareStatement("select * from song");
         	    ResultSet set=Statement.executeQuery();
         	    System.out.println("-------song list-----");
         	    while(set.next()) {
         	    	System.out.println(set.getString(1)+"||"+set.getString(2)+"||"+set.getDouble(3)+"||"+set.getString(4));
         	    	 }
         	 connection.close();
  	         	Statement.close();
      		}
      		
      		case 5->flag=false;
      		
      		default->System.out.println("invalid choice ....");
      			
      		
			
			
			}
			}
		
		}else {
			System.out.println("Invalid username or password....");
			
		}
		
	  
	}

	private static Connection dbConnect() throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.cj.jdbc.Driver");
		 Connection connection= DriverManager.getConnection("jdbc:mysql://localhost:3307/spotify?user=root&password=harry2004");
		return connection;
	}
	public static void ShowSongs() throws ClassNotFoundException, SQLException {
		Connection connection=dbConnect();
		 PreparedStatement statement1=connection.prepareStatement("select * from song");
		 ResultSet set1=statement1.executeQuery();
		 System.out.println("------Song List-----------");
		 System.out.println("-----------------------------");
		 while(set1.next()) {
			 System.out.println(set1.getString(1)+" | "+set1.getString(2)+" | "+set1.getDouble(3)+" | "+set1.getString(4));
		 }
	}

}
