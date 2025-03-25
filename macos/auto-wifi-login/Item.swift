//
//  Item.swift
//  auto-wifi-login
//
//  Created by Arjun Manjunath on 25/03/25.
//

import Foundation
import SwiftData

@Model
final class Item {
    var timestamp: Date
    
    init(timestamp: Date) {
        self.timestamp = timestamp
    }
}
