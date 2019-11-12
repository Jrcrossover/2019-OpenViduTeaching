package urjc.ovteaching.rooms;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import urjc.ovteaching.users.User;
import urjc.ovteaching.users.UserComponent;
import urjc.ovteaching.users.UserService;

@CrossOrigin
@RestController
public class RoomController {

	@Autowired
	private RoomService roomServ;

	@Autowired
	private UserService userServ;

	@Autowired
	private UserComponent userComponent;

	/**
	 * Creates a new room
	 * 
	 * @return the room name
	 */
	@PostMapping("/api/room/{roomName}")
	public ResponseEntity<String> createNewRoom(@PathVariable String roomName, HttpServletRequest request) {
		if (!request.isUserInRole("ADMIN")) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		} else {
			if (roomServ.findByName(roomName) != null) {
				return new ResponseEntity<>(HttpStatus.CONFLICT);
			}
			User currentUser = userComponent.getLoggedUser();
			currentUser = userServ.findByName(request.getUserPrincipal().getName());
			Room room = new Room(roomName);
			roomServ.addRoomWithMod(room, currentUser);
			return new ResponseEntity<>(room.getName(), HttpStatus.CREATED);
		}
	}

	/**
	 * Gets an invite code from a room
	 * 
	 * @param roomName the name of the room
	 * @param role     participant or moderator
	 * @return the invite code
	 */
	@GetMapping("/api/room/{roomName}/inviteCode/{role}")
	public ResponseEntity<String> getInviteCode(HttpServletRequest request, @PathVariable String roomName,
			@PathVariable String role) {
		Room room = roomServ.findByName(roomName);
		User user = userServ.findByName(request.getUserPrincipal().getName());
		if (room != null) {
			if (!room.isModerator(user)) {
				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			} else {
				if (role.equals("participant")) {
					return new ResponseEntity<>(room.getParticipantInviteCode(), HttpStatus.OK);
				} else if (role.equals("moderator")) {
					return new ResponseEntity<>(room.getModeratorInviteCode(), HttpStatus.OK);
				}
			}
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	/**
	 * Returns a room if the code exists
	 * 
	 * @return the room with either code (participant or moderator)
	 */
	@GetMapping("/api/room/{code}")
	public ResponseEntity<String> getRoomByInviteCode(@PathVariable String code) {
		Room room = roomServ.findByInviteCode(code);
		if (room == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(room.getName(), HttpStatus.OK);
	}

	/**
	 * Logs a user into a room
	 * 
	 * @return the room with either code (participant or moderator)
	 */
	@PutMapping("/api/room/{code}/user/{userName}")
	public ResponseEntity<User> newUserInRoom(HttpSession session, HttpServletRequest request,
			@PathVariable String code, @PathVariable String userName) {
		User user = userServ.findByName(userName);
		String password = "pass"; // TODO change pass
		Room room = roomServ.findByInviteCode(code);

		if (room == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		if (user == null) {
			// Creates user if it doesn't exist
			user = new User(userName, password, "ROLE_USER");
			userServ.save(user);
		} else {
			if (room.isModerator(user)) {
				// Cannot enter a room if already a mod of it
				return new ResponseEntity<>(HttpStatus.CONFLICT);
			}
			if (code.equals(room.getParticipantInviteCode()) && room.isParticipant(user)) {
				// Cannot enter a room as participant if already in it
				return new ResponseEntity<>(HttpStatus.CONFLICT);
			}
		}
		try {
			/*
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		    if (auth != null){    
		        new SecurityContextLogoutHandler().logout(request, response, auth);
		    }
		    */
			request.login(userName, password);
		} catch (ServletException e) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}
		if (!userComponent.isLoggedUser()) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}

		if (code.equals(room.getParticipantInviteCode())) {
			user.addParticipatedRoom(room);
		} else {
			roomServ.makeModerator(user, room);
			if (!user.getRoles().contains("ADMIN")) {
				// Makes user admin if they weren't
				user.addRole("ROLE_ADMIN");
			}
		}
		roomServ.save(room);
		userServ.save(user);
		return new ResponseEntity<>(user, HttpStatus.CREATED);
	}
}