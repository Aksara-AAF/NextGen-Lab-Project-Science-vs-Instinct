package com.nextgenlab.backend.controller;

import com.nextgenlab.backend.model.dto.RoomRequest;
import com.nextgenlab.backend.model.entity.GameRoom;
import com.nextgenlab.backend.service.RoomService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/room")
public class GameController {

    private final RoomService roomService;

    public GameController(RoomService roomService) {
        this.roomService = roomService;
    }


    @PostMapping("/create")
    public GameRoom createRoom(@RequestBody RoomRequest request) {
        return roomService.createRoom(request.getRole());
    }


    @PostMapping("/join/{code}")
    public GameRoom joinRoom(@PathVariable String code, @RequestBody RoomRequest request) {
        return roomService.joinRoom(code, request.getRole());
    }


    @GetMapping("/{code}")
    public GameRoom getRoomStatus(@PathVariable String code) {
        return roomService.getRoom(code);
    }
}
