import time
import logging
import threading

logger = logging.getLogger("GateController")

class GateController:
    def __init__(self, gate_id="gate1"):
        self.gate_id = gate_id
        self.is_gate_open = False
        self.lock = threading.Lock()
        self.timer = None
        self._print_gate_status()

    def open_gate(self, auto_close_delay=10.0):
        """
        Opens the simulated gate. Starts a thread to auto-close it after auto_close_delay seconds.
        """
        with self.lock:
            if self.is_gate_open:
                logger.info(f"Gate {self.gate_id} is already OPEN. Resetting auto-close timer.")
                if self.timer:
                    self.timer.cancel()
            else:
                self.is_gate_open = True
                logger.info(f"Gate {self.gate_id} is now [ OPENING... ]")
                self._print_gate_status()

            # Start timer to close gate
            self.timer = threading.Timer(auto_close_delay, self.close_gate)
            self.timer.start()

    def close_gate(self):
        """
        Closes the simulated gate.
        """
        with self.lock:
            if not self.is_gate_open:
                logger.info(f"Gate {self.gate_id} is already CLOSED.")
                return
            
            self.is_gate_open = False
            logger.info(f"Gate {self.gate_id} is now [ CLOSING... ]")
            self._print_gate_status()
            if self.timer:
                self.timer.cancel()
                self.timer = None

    def is_open(self):
        with self.lock:
            return self.is_gate_open

    def _print_gate_status(self):
        """
        Prints a visual representation of the gate state in the terminal.
        """
        status_str = "OPEN" if self.is_gate_open else "CLOSED"
        color_code = "\033[92m" if self.is_gate_open else "\033[91m" # Green for open, Red for closed
        reset_code = "\033[0m"
        
        border = "=" * 40
        gate_visual = ""
        if self.is_gate_open:
            gate_visual = """
                 /|
                / |  [ BARRIER UP ]
               /  |
              /   |
             /____|
            [ GATE ]
            """
        else:
            gate_visual = """
            +-------------------+
            |===================|  [ BARRIER DOWN ]
            +-------------------+
            | GATE              |
            """

        print(f"\n{color_code}{border}{reset_code}")
        print(f"{color_code}>>> GATE {self.gate_id.upper()} STATUS: {status_str} <<<{reset_code}")
        print(f"{color_code}{gate_visual}{reset_code}")
        print(f"{color_code}{border}{reset_code}\n")

    def cleanup(self):
        """
        Cleans up any running timers.
        """
        with self.lock:
            if self.timer:
                self.timer.cancel()
                self.timer = None
            logger.info(f"GateController {self.gate_id} cleaned up.")
